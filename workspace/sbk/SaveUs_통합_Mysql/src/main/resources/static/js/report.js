/* src/main/resources/static/js/report.js */

const COLOR_PRIMARY = '#1DAB87';
const COLOR_DANGER = '#FF6B6B';
const COLOR_WARNING = '#FFC857';
const COLOR_INFO = '#4A90E2'; // 파랑 (EXCELLENT 등)

// 차트 변수들
let scoreChart, radarChart, obesityChart, bmiChart;
let carbChart, proteinChart, fatChart;

document.addEventListener('DOMContentLoaded', () => {
    const today = new Date().toISOString().split('T')[0];

    const dietInput = document.getElementById('dietDate');
    const obesityInput = document.getElementById('obesityDate');
    if (dietInput) dietInput.value = today;
    if (obesityInput) obesityInput.value = today;

    loadDietSection(today);
    loadObesitySection(today);
    loadCommonData(today);

    if (dietInput) {
        dietInput.addEventListener('change', (e) => {
            loadDietSection(e.target.value);
        });
    }

    if (obesityInput) {
        obesityInput.addEventListener('change', (e) => {
            loadObesitySection(e.target.value);
        });
    }
});

// ==========================================
// 1. 식단 분석 로드 (레이더 + 코멘트 + 색상 변경)
// ==========================================
function loadDietSection(date) {
    fetch(`/api/report/daily?date=${date}`)
        .then(res => res.json())
        .then(data => {
            const chartArea = document.getElementById('dietChartArea');
            const noDataMsg = document.getElementById('dietNoData');
            const commentBox = document.getElementById('aiComment');
            const riskText = document.getElementById('diabetesText');

            if (!data.hasData) {
                chartArea.style.display = 'none';
                noDataMsg.style.display = 'block';
                riskText.innerText = '-';
                riskText.className = 'risk-score'; // 클래스 초기화
                commentBox.innerText = '해당 날짜에는 기록된 식단이 없습니다.';
            } else {
                chartArea.style.display = 'block';
                noDataMsg.style.display = 'none';

                const rLevel = data.diabetesRiskLevel ? data.diabetesRiskLevel : '';

                // [추가] 등급별 텍스트 색상 적용
                riskText.className = 'risk-score'; // 초기화
                if (rLevel === 'EXCELLENT') riskText.classList.add('text-excellent');
                else if (rLevel === 'GOOD') riskText.classList.add('text-good');
                else if (rLevel === 'NORMAL') riskText.classList.add('text-normal');
                else if (rLevel === 'WARNING') riskText.classList.add('text-warning');
                else if (rLevel === 'DANGER') riskText.classList.add('text-danger');

                riskText.innerText = `당뇨 예방 점수: ${data.diabetesScore}점 (${rLevel})`;

                if (data.diabetesComment) {
                    commentBox.innerHTML = `<strong>[당뇨 위험도                     분석 결과]</strong> ${data.diabetesComment}`;
                } else {
                    commentBox.innerText = "분석 데이터가 없습니다.";
                }

                updateRadarChart(data);
            }
        })
        .catch(err => console.error("Diet load error:", err));
}

// ==========================================
// 2. 비만 위험도 로드 (색상 변경 로직 추가)
// ==========================================
function loadObesitySection(date) {
    fetch(`/api/report/daily?date=${date}`)
        .then(res => res.json())
        .then(data => {
            const prob = parseInt(data.obesityProbability || 0);

            // 텍스트 색상도 같이 변경
            const percentText = document.getElementById('obesityPercent');
            percentText.innerText = `${prob}%`;
            percentText.style.color = getObesityColor(prob);

            updateObesityChart(prob);
        })
        .catch(err => console.error("Obesity load error:", err));
}

// ==========================================
// 3. 공통 트렌드 로드
// ==========================================
function loadCommonData(date) {
    fetch(`/api/report/daily?date=${date}`)
        .then(res => res.json())
        .then(data => {
            updateCommonCharts(data);
        })
        .catch(err => console.error("Common load error:", err));
}


// ---------------------------------------------------
// [차트 업데이트 함수들]
// ---------------------------------------------------

function updateRadarChart(data) {
    const ctxRadar = document.getElementById('diabetesRadarChart');
    if (radarChart) radarChart.destroy();

    const myIntake = data.radarMyIntake || [0, 0, 0, 0, 0];
    const maxVal = Math.max(...myIntake);
    let suggestedMax = 120;
    let stepSize = 20;

    if (maxVal > 200) {
        suggestedMax = Math.ceil(maxVal / 50) * 50;
        stepSize = 50;
    }

    radarChart = new Chart(ctxRadar, {
        type: 'radar',
        data: {
            labels: ['탄수화물', '단백질', '지방', '당류', '나트륨'],
            datasets: [{
                label: '나의 섭취(%)',
                data: data.radarMyIntake || [0, 0, 0, 0, 0],
                borderColor: COLOR_DANGER,
                backgroundColor: 'rgba(255, 107, 107, 0.2)',
                borderWidth: 2,
                pointRadius: 3
            }, {
                label: '권장(%)',
                data: data.radarGoal || [100, 100, 100, 100, 100],
                borderColor: COLOR_PRIMARY,
                backgroundColor: 'transparent',
                borderDash: [5, 5],
                pointRadius: 0
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            scales: {
                r: {
                    suggestedMin: 0,
                    suggestedMax: suggestedMax,
                    pointLabels: { font: { size: 12 } },
                    // [추가] 퍼센트 눈금 표시 설정
                    ticks: {
                        display: true,
                        stepSize: stepSize,
                        backdropColor: 'transparent', // 숫자 뒤 배경 투명
                        font: { size: 10 }
                    }
                }
            },
            plugins: { legend: { position: 'top' } }
        }
    });
}

// [추가] 비만도 색상 결정 함수
function getObesityColor(prob) {
    if (prob <= 20) return '#3B82F6'; // 파랑 (안전)
    if (prob <= 40) return '#1DAB87'; // 초록 (양호)
    if (prob <= 60) return '#FFC107'; // 노랑 (주의)
    if (prob <= 80) return '#FF9F43'; // 주황 (위험)
    return '#FF6B6B';                 // 빨강 (매우 위험)
}

function updateObesityChart(probability) {
    const ctxObesity = document.getElementById('obesityRiskChart');
    if (obesityChart) obesityChart.destroy();

    // 위험도에 따른 색상 가져오기
    const riskColor = getObesityColor(probability);

    obesityChart = new Chart(ctxObesity, {
        type: 'doughnut',
        data: {
            labels: ['위험도', '안전'],
            datasets: [{
                data: [probability, 100 - probability],
                backgroundColor: [riskColor, '#EEEEEE'], // 동적 색상 적용
                borderWidth: 0,
                hoverOffset: 0
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            cutout: '85%',
            plugins: {
                legend: { display: false },
                tooltip: { enabled: false }
            },
            animation: {
                animateScale: true,
                animateRotate: true
            }
        }
    });
}

function updateCommonCharts(data) {
    // 1. 건강 점수
    document.getElementById('avgScoreBadge').innerText = `평균 ${data.averageScore}점`;
    const ctxScore = document.getElementById('scoreChart');
    if (scoreChart) scoreChart.destroy();

    scoreChart = new Chart(ctxScore, {
        type: 'line',
        data: {
            labels: data.scoreDates || [],
            datasets: [{
                label: '점수',
                data: data.scoreValues || [],
                borderColor: COLOR_PRIMARY,
                backgroundColor: 'rgba(29, 171, 135, 0.1)',
                fill: true,
                tension: 0,
                pointRadius: 4,
                pointBackgroundColor: '#fff',
                pointBorderColor: COLOR_PRIMARY
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            scales: {
                y: { min: 0, max: 100 },
                x: { grid: { display: false } }
            },
            plugins: { legend: { display: false } }
        }
    });

    // 2. BMI & 체중
    const ctxBmi = document.getElementById('bmiChart');
    if (bmiChart) bmiChart.destroy();

    const weightList = data.weightValues || [];
    const bmiData = data.bmiValues || [];

    let yMin = 10, yMax = 35;
    if (bmiData.length > 0) {
        const validValues = bmiData.filter(v => v !== null && v !== undefined);
        if (validValues.length > 0) {
            const minVal = Math.min(...validValues);
            const maxVal = Math.max(...validValues);
            yMin = Math.floor(minVal - 2);
            yMax = Math.ceil(maxVal + 2);
            if (yMin < 0) yMin = 0;
        }
    }

    bmiChart = new Chart(ctxBmi, {
        type: 'line',
        data: {
            labels: data.scoreDates || [],
            datasets: [{
                label: 'BMI',
                data: bmiData,
                borderColor: COLOR_INFO,
                backgroundColor: 'rgba(74, 144, 226, 0.1)',
                fill: true,
                tension: 0,
                pointRadius: 4,
                pointBackgroundColor: '#fff',
                pointBorderColor: COLOR_INFO
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            scales: {
                y: { beginAtZero: false, min: yMin, max: yMax, ticks: { stepSize: 1 } },
                x: { grid: { display: false } }
            },
            plugins: {
                legend: { display: false },
                tooltip: {
                    callbacks: {
                        label: function(context) {
                            let label = context.dataset.label || '';
                            if (label) label += ': ';
                            if (context.parsed.y !== null) label += context.parsed.y;
                            const w = weightList[context.dataIndex];
                            if (w) return [label, `체중: ${w} kg`];
                            return label;
                        }
                    }
                }
            }
        }
    });

    // 3. 식단 유형 (계단형)
    const createStepChart = (ctxId, chartRef, label, dates, codes, color) => {
        const ctx = document.getElementById(ctxId);
        if (chartRef) chartRef.destroy();
        return new Chart(ctx, {
            type: 'line',
            data: {
                labels: dates || [],
                datasets: [{
                    label: label,
                    data: codes || [],
                    borderColor: color,
                    stepped: true,
                    fill: false,
                    borderWidth: 2,
                    pointRadius: 0
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                scales: {
                    y: {
                        min: 0, max: 4,
                        ticks: {
                            stepSize: 1,
                            font: { size: 10 },
                            callback: function(val) {
                                if (val === 1) return '저';
                                if (val === 2) return '균형';
                                if (val === 3) return '고';
                                return '';
                            }
                        },
                        grid: { drawBorder: false }
                    },
                    x: { display: false }
                },
                plugins: {
                    legend: { display: false },
                    title: { display: true, text: label, align: 'start', font: { size: 13 }, padding: { bottom: 5 } }
                }
            }
        });
    };

    carbChart = createStepChart('carbChart', carbChart, '탄수화물', data.dietDates, data.carbCodes, '#FFC857');
    proteinChart = createStepChart('proteinChart', proteinChart, '단백질', data.dietDates, data.proteinCodes, '#1DAB87');
    fatChart = createStepChart('fatChart', fatChart, '지방', data.dietDates, data.fatCodes, '#FF6B6B');

    // 4. Top 5
    const ul = document.getElementById('topMealsList');
    ul.innerHTML = '';
    if (data.topMeals && data.topMeals.length > 0) {
        data.topMeals.forEach((m, i) => {
            const li = document.createElement('li');
            const category = m.mealTime ? `(${m.mealTime})` : '';
            li.innerHTML = `<strong>${i + 1}. ${m.mealName}</strong> <span style="color:#888; font-size:0.9em;">${category}</span>`;
            ul.appendChild(li);
        });
    } else {
        ul.innerHTML = '<p style="color:#999; text-align:center; padding: 10px;">기록 없음</p>';
    }
}