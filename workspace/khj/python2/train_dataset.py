# train_dataset.py
import pandas as pd
from sklearn.preprocessing import StandardScaler
from sklearn.cluster import KMeans
import joblib


# 1. CSV 로드
def load_food_data(path="food_nutrition.csv"):
    df = pd.read_csv(path)

    df = df[[
        "food_name",
        "category",
        "calories_kcal",
        "carbs_g",
        "protein_g",
        "fat_g",
        "fiber_g",
        "sodium_mg"
    ]]
    return df


# 2. 스케일러 학습
def train_scaler(df):
    features = df[["calories_kcal", "carbs_g", "protein_g", "fat_g"]]

    scaler = StandardScaler()
    scaler.fit(features)

    joblib.dump(scaler, "food_recommend_scaler.pkl")
    print("스케일러 저장 완료: food_recommend_scaler.pkl")

    scaled = scaler.transform(features)
    return scaled


# 3. KMeans 학습
def train_kmeans(scaled_data, df, n_clusters=6):
    model = KMeans(n_clusters=n_clusters, random_state=42)
    clusters = model.fit_predict(scaled_data)

    df["cluster"] = clusters

    joblib.dump(model, "food_recommend_model.pkl")
    df.to_csv("food_clustered.csv", index=False)

    print("KMeans 모델 저장 완료: food_recommend_model.pkl")
    return model


# 4. 학습 파이프라인
if __name__ == "__main__":
    df = load_food_data()

    scaled = train_scaler(df)

    train_kmeans(scaled, df)

    print("학습 완료")
