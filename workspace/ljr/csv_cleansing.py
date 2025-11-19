import pandas as pd
import numpy as np

df = pd.read_csv('food_nutrition_origin.csv')

food_name_duplicated = df['food_name'].duplicated()
print(df[food_name_duplicated])

def convert_name(x):
    x = str(x)
    if "_" not in x:
        return x
    parts = x.split("_", 1)
    x =  f"{parts[0]}({parts[1]})"

    if "_" in x:
        x = x.replace("_", " ")


    return x

# _를 (문자)로 바꿈
df['food_name'] = df['food_name'].apply(convert_name)
df = df.drop_duplicates(subset=['food_name'])

# 중복제거 및 결측치 채우기
df = df.drop_duplicates(subset=['food_name'])
df = df.fillna('0')

# 음식 이름과 / 카테고리를 제외한 반올림
exclude_cols = ['food_name', 'category']
numeric_cols = [col for col in df.columns if col not in exclude_cols]
df[numeric_cols] = df[numeric_cols].round().astype(int)
print(df.head())

# csv로 저장
result = df.to_csv('food_nutrition.csv', index=False)
print(pd.read_csv('food_nutrition.csv'))

