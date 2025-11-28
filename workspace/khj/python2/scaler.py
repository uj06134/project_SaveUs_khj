# scaler.py
import joblib

# 학습된 스케일러 로드
try:
    scaler = joblib.load("food_recommend_scaler.pkl")
    print("스케일러 로드 완료: food_recommend_scaler.pkl")
except:
    raise Exception("food_recommend_scaler.pkl 파일을 찾을 수 없습니다.")


# 음식 특징 스케일링 함수
def scale_food_features(df):
    return scaler.transform(df[["calories_kcal", "carbs_g", "protein_g", "fat_g"]])
