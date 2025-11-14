from fastapi import FastAPI, File, UploadFile, HTTPException
from pydantic import BaseModel
from starlette.middleware.cors import CORSMiddleware

test = FastAPI()

test.add_middleware(
    CORSMiddleware,
    allow_origins=["http://127.0.0.1"],
    allow_credentials=True,
    allow_methods=["GET", "POST", "PUT", "DELETE"],
    allow_headers=["*"],
)


@test.post("/api_test")
async def api_test(
        file: UploadFile = File(...),
):
    if file is None:
        raise HTTPException(status_code=400, detail="파일 없음")

    if file.content_type not in ["image/png", "image/jpeg"]:
        raise HTTPException(status_code=400, detail="잘못된 파일 유형")

    content = await file.read()

    if len(content) == 0:
        raise HTTPException(status_code=400, detail="빈 파일")

    # 파일 확인용 코드
    with open("test.png", "wb") as f:
        f.write(content)

    return {
         "items": [
        {
            "name": "김밥 1줄",
            "kcal": 450,
            "carbs": 55,
            "protein": 7,
            "fat": 8
        },
        {
            "name": "떡볶이",
            "kcal": 459,
            "carbs": 102,
            "protein": 9,
            "fat": 2
        }
        ]
    }
