/*
document.addEventListener("DOMContentLoaded", function () {

    const btn = document.getElementById("mealAddBtn");
    const input = document.getElementById("mealSearchInput");
    const box = document.getElementById("autocompleteBox");

    if (!btn || !input) {
        console.log("버튼 또는 입력창을 찾을 수 없습니다.");
        return;
    }

    // 자동완성 기능
    input.addEventListener("input", async function () {
        const keyword = input.value.trim();

        if (keyword.length === 0) {
            box.innerHTML = "";
            box.style.display = "none";
            return;
        }

        try {
            const res = await fetch(`/food/autocomplete?keyword=${encodeURIComponent(keyword)}`);
            const names = await res.json();

            if (!names || names.length === 0) {
                box.innerHTML = "";
                box.style.display = "none";
                return;
            }

            let html = "";
            names.forEach(n => {
                html += `<div class="autocomplete-item" data-name="${n}">${n}</div>`;
            });

            box.innerHTML = html;
            box.style.display = "block";

        } catch (e) {
            console.error(e);
        }
    });

    // 자동완성 선택 처리
    box.addEventListener("click", function (event) {
        if (event.target.classList.contains("autocomplete-item")) {
            const name = event.target.getAttribute("data-name");
            input.value = name;
            box.innerHTML = "";
            box.style.display = "none";
        }
    });

    // 추가 버튼 클릭 → 기존 기능 그대로 유지
    btn.addEventListener("click", async function () {

        // 자동완성 박스 닫기
        box.innerHTML = "";
        box.style.display = "none";

        const keyword = input.value.trim();
        if (keyword === "") {
            alert("음식명을 입력해주세요.");
            return;
        }

        try {
            // 음식 검색 요청
            const res = await fetch("/food/search?keyword=" + encodeURIComponent(keyword));
            const foods = await res.json();

            if (!foods || foods.length === 0) {
                alert("검색 결과가 없습니다.");
                return;
            }

            const food = foods[0];

            // 서버에 저장 요청
            const saveRes = await fetch("/meal/add-from-food", {
                method: "POST",
                headers: {"Content-Type": "application/json"},
                body: JSON.stringify({
                    mealName: food.foodName,
                    calories: food.caloriesKcal,
                    carbs: food.carbsG,
                    protein: food.proteinG,
                    fat: food.fatG,
                    sugar: food.sugarG,
                    fiber: food.fiberG,
                    calcium: food.calciumMg,
                    sodium: food.sodiumMg
                })
            });

            const result = await saveRes.text();

            if (result === "OK") {
                alert("식사 목록에 추가되었습니다");
                location.reload();
            } else {
                alert("추가 중 오류가 발생했습니다: " + result);
            }

        } catch (e) {
            console.error(e);
            alert("요청 처리 중 오류가 발생했습니다.");
        }
    });
});
*/



document.addEventListener("DOMContentLoaded", function () {

    // ---------------------------------------------------------
    // 1. 기존 검색 기능
    // ---------------------------------------------------------
    const btn = document.getElementById("mealAddBtn");
    const input = document.getElementById("mealSearchInput");
    const box = document.getElementById("autocompleteBox");

    if (btn && input) {
        input.addEventListener("input", async function () {
            const keyword = input.value.trim();
            if (keyword.length === 0) {
                box.innerHTML = "";
                box.style.display = "none";
                return;
            }
            try {
                const res = await fetch(`/food/autocomplete?keyword=${encodeURIComponent(keyword)}`);
                const names = await res.json();
                if (!names || names.length === 0) {
                    box.innerHTML = "";
                    box.style.display = "none";
                    return;
                }
                let html = "";
                names.forEach(n => {
                    html += `<div class="autocomplete-item" data-name="${n}">${n}</div>`;
                });
                box.innerHTML = html;
                box.style.display = "block";
            } catch (e) { console.error(e); }
        });

        box.addEventListener("click", function (event) {
            if (event.target.classList.contains("autocomplete-item")) {
                input.value = event.target.getAttribute("data-name");
                box.innerHTML = "";
                box.style.display = "none";
            }
        });

        btn.addEventListener("click", async function () {
            box.innerHTML = "";
            box.style.display = "none";
            const keyword = input.value.trim();
            if (keyword === "") { alert("음식명을 입력해주세요."); return; }

            try {
                const res = await fetch("/food/search?keyword=" + encodeURIComponent(keyword));
                const foods = await res.json();
                if (!foods || foods.length === 0) { alert("검색 결과가 없습니다."); return; }

                const success = await saveMealRequest(foods[0]);
                if(success) {
                    alert("저장되었습니다.");
                    location.reload();
                }
            } catch (e) {
                console.error(e);
                alert("오류 발생");
            }
        });
    }

    // ---------------------------------------------------------
    // 2. [수정됨] AI 사진 업로드 (시간차 저장 방식)
    // ---------------------------------------------------------
    const aiInput = document.getElementById("aiFileInput");
    const aiBtn = document.querySelector(".photo-upload-btn");

    // ★ 시간 지연을 위한 헬퍼 함수 (ms초 만큼 대기)
    const delay = (ms) => new Promise(resolve => setTimeout(resolve, ms));

    if (aiBtn && aiInput) {
        aiBtn.addEventListener("click", () => aiInput.click());

        aiInput.addEventListener("change", async function(event) {
            const file = event.target.files[0];
            if (!file) return;

            const formData = new FormData();
            formData.append('file', file);

            alert("AI가 사진을 분석 중입니다... 잠시만 기다려주세요.");

            try {
                const response = await fetch('/meal/ai-upload', {
                    method: 'POST',
                    body: formData
                });

                if (!response.ok) throw new Error("서버 오류");

                const dataList = await response.json();

                if (dataList && dataList.length > 0) {
                    const names = dataList.map(d => d.mealName).join(", ");
                    const msg = `[분석 결과]\n감지된 음식: ${names}\n\n총 ${dataList.length}개의 음식을 모두 저장하시겠습니까?`;

                    if (confirm(msg)) {
                        // ★ 순차 저장 시작 (하나씩 await로 기다리면서 저장) ★
                        for (const item of dataList) {
                            await saveMealRequest(item); // 저장 요청
                            await delay(300);            // 0.3초 쉬기 (시간 충돌 방지)
                        }

                        alert("모두 저장되었습니다.");
                        location.reload();
                    }
                } else {
                    alert("음식을 인식하지 못했습니다.");
                }

            } catch (err) {
                console.error(err);
                alert("분석 중 오류가 발생했습니다.");
            } finally {
                aiInput.value = "";
            }
        });
    }

    // ---------------------------------------------------------
    // 공통 저장 요청 함수
    // ---------------------------------------------------------
    async function saveMealRequest(mealData) {
        try {
            const payload = {
                mealName: mealData.mealName || mealData.foodName,
                calories: mealData.calories || mealData.caloriesKcal,
                carbs: mealData.carbs || mealData.carbsG,
                protein: mealData.protein || mealData.proteinG,
                fat: mealData.fat || mealData.fatG,
                sugar: mealData.sugar || mealData.sugarG,
                fiber: mealData.fiber || mealData.fiberG,
                calcium: mealData.calcium || mealData.calciumMg,
                sodium: mealData.sodium || mealData.sodiumMg
            };

            const saveRes = await fetch("/meal/add-from-food", {
                method: "POST",
                headers: {"Content-Type": "application/json"},
                body: JSON.stringify(payload)
            });

            const result = await saveRes.text();
            return (result === "OK");

        } catch (e) {
            console.error("저장 실패:", e);
            return false;
        }
    }
});