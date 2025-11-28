from fastapi import FastAPI
from pydantic import BaseModel
import pandas as pd
import joblib

from food_recommender import (
    filter_processed,
    map_deficit_to_cluster
)

app = FastAPI()

# ------------------------------------------------------------
# 1. 음식 DB + 모델 로드
# ------------------------------------------------------------
try:
    df_food = pd.read_csv("food_clustered.csv")
    model = joblib.load("food_recommend_model.pkl")
    print("모델 로드 완료: food_recommend_model.pkl")
except:
    raise Exception("food_clustered.csv 또는 food_recommend_model.pkl 파일을 찾을 수 없습니다.")


# ------------------------------------------------------------
# 2. 요청 형식 정의
# ------------------------------------------------------------
class RecommendRequest(BaseModel):
    goal_calories: float
    goal_carbs: float
    goal_protein: float
    goal_fat: float

    current_calories: float
    current_carbs: float
    current_protein: float
    current_fat: float
    current_fiber: float
    current_sodium: float


# ------------------------------------------------------------
# 3. 목표 대비 부족 비율 분석 함수
# ------------------------------------------------------------
def detect_deficit_by_goal(goal, current):
    deficit_list = []

    # 안전 나눗셈
    def ratio(cur, tgt):
        if tgt == 0:
            return 1
        return cur / tgt

    protein_rate = ratio(current["protein"], goal["goal_protein"])
    carbs_rate   = ratio(current["carbs"], goal["goal_carbs"])
    fat_rate     = ratio(current["fat"], goal["goal_fat"])
    cal_rate     = ratio(current["calories"], goal["goal_calories"])

    # 1) 단백질
    if protein_rate < 0.3:
        deficit_list.append("high_protein")
    elif protein_rate < 0.6:
        deficit_list.append("mid_protein")

    # 2) 식이섬유(기준 고정)
    if current["fiber"] < 20:
        deficit_list.append("high_fiber")

    # 3) 탄수화물
    if carbs_rate < 0.3:
        deficit_list.append("high_carbs")
    elif carbs_rate < 0.6:
        deficit_list.append("mid_carbs")

    # 4) 지방
    if fat_rate < 0.3:
        deficit_list.append("high_fat")
    elif fat_rate < 0.6:
        deficit_list.append("mid_fat")

    # 5) 칼로리
    if cal_rate < 0.3:
        deficit_list.append("high_calorie")
    elif cal_rate < 0.6:
        deficit_list.append("mid_calorie")

    return deficit_list


# ------------------------------------------------------------
# 4. 음식 추천 로직
# ------------------------------------------------------------
def recommend_menu(goal, current, df):

    df_filtered = filter_processed(df)

    deficit = detect_deficit_by_goal(goal, current)
    cluster_target = map_deficit_to_cluster(deficit)

    # 부족 요소가 없으면 전체 중 랜덤 추천
    if cluster_target is None:
        return df_filtered.sample(5)[["food_name", "category"]].to_dict(orient="records")

    recommended = df_filtered[df_filtered["cluster"] == cluster_target]

    if len(recommended) < 5:
        recommended = df_filtered.sample(5)

    return recommended.sample(5)[["food_name", "category"]].to_dict(orient="records")


# ------------------------------------------------------------
# 5. FastAPI 엔드포인트
# ------------------------------------------------------------
@app.post("/food/recommend")
def recommend(request: RecommendRequest):

    goal = {
        "goal_calories": request.goal_calories,
        "goal_carbs": request.goal_carbs,
        "goal_protein": request.goal_protein,
        "goal_fat": request.goal_fat
    }

    current = {
        "calories": request.current_calories,
        "carbs": request.current_carbs,
        "protein": request.current_protein,
        "fat": request.current_fat,
        "fiber": request.current_fiber,
        "sodium": request.current_sodium
    }

    result = recommend_menu(goal, current, df_food)

    return {
        "recommended": result,
        "deficit": detect_deficit_by_goal(goal, current)
    }
