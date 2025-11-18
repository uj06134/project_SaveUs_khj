// /static/js/challenges.js

document.addEventListener('DOMContentLoaded', () => {

    const tabsContainer = document.querySelector('.challenge-tabs');
    const tabContents = document.querySelectorAll('.tab-content');

    // 1. 탭 클릭 이벤트 (나의 챌린지 <-> 둘러보기)
    if (tabsContainer) {
        tabsContainer.addEventListener('click', (e) => {
            const clickedTab = e.target.closest('.tab-btn');
            if (!clickedTab) return;

            // 이미 활성화된 탭이면 무시
            if (clickedTab.classList.contains('active')) return;

            // data-tab 속성값 (예: "my-challenges-tab")
            const tabId = clickedTab.dataset.tab;
            const targetContent = document.getElementById(tabId);

            if (!targetContent) return;

            // 모든 탭 버튼 비활성화
            tabsContainer.querySelectorAll('.tab-btn').forEach(btn => {
                btn.classList.remove('active');
            });
            // 클릭된 탭 버튼 활성화
            clickedTab.classList.add('active');

            // 모든 탭 컨텐츠 숨기기
            tabContents.forEach(content => {
                content.classList.remove('active'); // (display: none)
            });
            // 타겟 탭 컨텐츠 보여주기
            targetContent.classList.add('active'); // (display: flex)
        });
    }

    // 2. "나의 챌린지"가 비어있을 때 "둘러보기" 버튼 클릭 이벤트
    const exploreNowBtn = document.querySelector('.btn-explore-now');
    if (exploreNowBtn) {
        exploreNowBtn.addEventListener('click', () => {
            // '둘러보기' 탭 버튼을 찾아서 강제로 클릭
            const exploreTabButton = document.querySelector('.tab-btn[data-tab="explore-tab"]');
            if (exploreTabButton) {
                exploreTabButton.click();
            }
        });
    }

});