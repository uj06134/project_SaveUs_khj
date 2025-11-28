from fastapi import FastAPI
from pydantic import BaseModel
import pandas as pd
import joblib
import pymysql
import mysql.connector

from food_recommender import (
    filter_processed,
    map_deficit_to_cluster
)

app = FastAPI()

# ------------------------------------------------------------
# 1. MySQL 연결 함수
# ------------------------------------------------------------
def get_connection():
    return pymysql.connect(
        host="3.37.90.119",
        user="root",
        password="3306",
        database="saveus",
        charset="utf8mb4",
        cursorclass=pymysql.cursors.DictCursor
    )

# ------------------------------------------------------------
# 2. FOOD_NUTRITION 데이터 로드
# ------------------------------------------------------------
def load_food_data():
    conn = get_connection()
    query = """
        SELECT 
            FOOD_NAME AS food_name,
            CATEGORY AS category,
            CALORIES_KCAL AS calories_kcal,
            CARBS_G AS carbs_g,
            PROTEIN_G AS protein_g,
            FATS_G AS fat_g,
            FIBER_G AS fiber_g,
            SODIUM_MG AS sodium_mg
        FROM FOOD_NUTRITION
    """
    df = pd.read_sql(query, conn)
    conn.close()
    return df


# ------------------------------------------------------------
# 3. 사용자 목표 조회
# ------------------------------------------------------------
def get_user_goal(user_id: int):
    conn = get_connection()
    query = f"""
        SELECT 
            CALORIES_KCAL AS goal_calories,
            PROTEIN_G AS goal_protein,
            CARBS_G AS goal_carbs,
            FATS_G AS goal_fat
        FROM USER_GOAL
        WHERE USER_ID = {user_id}
    """
    df = pd.read_sql(query, conn)
    conn.close()

    return df.to_dict(orient="records")[0] if not df.empty else None


# ------------------------------------------------------------
# 4. 오늘 섭취량 조회
# ------------------------------------------------------------
def get_today_nutrition(user_id: int):
    conn = get_connection()
    query = f"""
        SELECT 
            SUM(calories_kcal) AS calories,
            SUM(carbs_g) AS carbs,
            SUM(protein_g) AS protein,
            SUM(fat_g) AS fat,
            SUM(fiber_g) AS fiber,
            SUM(sodium_mg) AS sodium
        FROM MEAL_ENTRY
        WHERE USER_ID = {user_id}
          AND DATE(EAT_DATE) = CURDATE()
    """
    df = pd.read_sql(query, conn)
    conn.close()

    return df.to_dict(orient="records")[0]


# ------------------------------------------------------------
# 5. 부족 비율 계산
# ------------------------------------------------------------
def detect_deficit_by_goal(goal, current):
    deficit_list = []

    def ratio(cur, tgt):
        if tgt == 0:
            return 1
        return cur / tgt

    protein_rate = ratio(current["protein"], goal["goal_protein"])
    carbs_rate   = ratio(current["carbs"], goal["goal_carbs"])
    fat_rate     = ratio(current["fat"], goal["goal_fat"])
    cal_rate     = ratio(current["calories"], goal["goal_calories"])

    if protein_rate < 0.3:
        deficit_list.append("high_protein")
    elif protein_rate < 0.6:
        deficit_list.append("mid_protein")

    if current["fiber"] < 20:
        deficit_list.append("high_fiber")

    if carbs_rate < 0.3:
        deficit_list.append("high_carbs")
    elif carbs_rate < 0.6:
        deficit_list.append("mid_carbs")

    if fat_rate < 0.3:
        deficit_list.append("high_fat")
    elif fat_rate < 0.6:
        deficit_list.append("mid_fat")

    if cal_rate < 0.3:
        deficit_list.append("high_calorie")
    elif cal_rate < 0.6:
        deficit_list.append("mid_calorie")

    return deficit_list


# ------------------------------------------------------------
# 6. 모델 및 음식 DB 로드
# ------------------------------------------------------------
df_food = load_food_data()
model = joblib.load("food_recommend_model.pkl")


# ------------------------------------------------------------
# 7. 추천 로직
# ------------------------------------------------------------
def recommend_menu(goal, current, df):
    df_filtered = filter_processed(df)

    deficit = detect_deficit_by_goal(goal, current)
    cluster_target = map_deficit_to_cluster(deficit)

    if cluster_target is None:
        return df_filtered.sample(5)[["food_name", "category"]].to_dict(orient="records")

    recommended = df_filtered[df_filtered["cluster"] == cluster_target]

    if len(recommended) < 5:
        recommended = df_filtered.sample(5)

    return recommended.sample(5)[["food_name", "category"]].to_dict(orient="records")


# ------------------------------------------------------------
# 8. FastAPI 엔드포인트
# ------------------------------------------------------------
class RecommendRequest(BaseModel):
    user_id: int


@app.post("/food/recommend")
def recommend(request: RecommendRequest):

    goal = get_user_goal(request.user_id)
    if goal is None:
        return {"error": f"{request.user_id}번 사용자의 목표 데이터가 없습니다."}

    current = get_today_nutrition(request.user_id)
    if current is None or current["calories"] is None:
        return {"error": f"{request.user_id}번 사용자의 오늘 섭취 기록이 없습니다."}

    result = recommend_menu(goal, current, df_food)

    return {
        "user_id": request.user_id,
        "goal": goal,
        "current": current,
        "deficit": detect_deficit_by_goal(goal, current),
        "recommended": result
    }
