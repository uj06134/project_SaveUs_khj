# food_recommender.py

# --------------------------------------------------------
# 1. 가공식품 제거
# --------------------------------------------------------
def filter_processed(df):
    ban_keywords = ["간편", "조리", "밀키트", "즉석", "레토르트", "HMR", "세트"]

    for word in ban_keywords:
        df = df[~df["category"].str.contains(word, na=False)]
        df = df[~df["food_name"].str.contains(word, na=False)]

    return df


# --------------------------------------------------------
# 2. 부족 영양소 → 클러스터 번호 매핑
# --------------------------------------------------------
def map_deficit_to_cluster(deficit_list):

    cluster_map = {
        "high_protein": 2,     # 고단백
        "high_fiber": 4,       # 고식이섬유
        "high_calorie": 1,     # 고칼로리
        "high_carbs": 3,       # 고탄수
        "high_fat": 5          # 고지방
    }

    if not deficit_list:
        return None

    return cluster_map.get(deficit_list[0], None)
