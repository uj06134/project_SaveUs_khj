from six import BytesIO
from ultralytics import YOLO
from collections import Counter
from PIL import Image

# YOLO 모델 로드
model = YOLO("saveUs_food_detection.pt")

async def detect_objects(content: bytes) -> Counter:
    # 이미지 열기
    img = Image.open(BytesIO(content))

    # YOLO 추론
    pred = model.predict(img, verbose=False)[0]

    # 감지된 박스가 없으면 빈 결과 반환
    if pred.boxes is None or pred.boxes.cls is None:
        return Counter()

    # 클래스 ID 배열 추출
    cls_ids = pred.boxes.cls.cpu().numpy().astype(int)

    # YOLO 모델 내부 클래스 이름 매핑
    names = model.model.names  # 예: {0: "apple", 1: "banana"}
    labels = [names[i] for i in cls_ids]

    return Counter(labels)


if __name__ == "__main__":
    img = Image.open("sample.jpg")
    result = model.predict(img)[0]
    result.show()
