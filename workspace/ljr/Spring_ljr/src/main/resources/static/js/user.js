function updateEmail() {
    const id = document.getElementById("emailId").value;
    const domain = document.getElementById("emailDomain").value;

    const full = id && domain ? id + "@" + domain : "";
    document.getElementById("emailInput").value = full;
}

function resetEmailCheck() {
    const result = document.getElementById("emailCheckResult");
    result.innerText = "";
    result.style.color = "";
}

function checkEmail() {
    const email = document.getElementById("emailInput").value;
    const result = document.getElementById("emailCheckResult");

    if (!email) {
        result.innerText = "이메일을 입력해주세요.";
        result.style.color = "red";
        return;
    }

    /* ★ 올바른 URL로 수정됨 */
    fetch(`/user/checkEmail?email=${email}`)
        .then(res => res.text())              // ← 문자열 받기
        .then(status => {
            if (status === "duplicate") {
                result.innerText = "이미 사용 중인 이메일입니다.";
                result.style.color = "red";
            } else {
                result.innerText = "사용 가능한 이메일입니다.";
                result.style.color = "green";
            }
        });
}

/* 이메일 복원 기능 */
window.addEventListener('load', function () {
    const full = document.getElementById('emailInput').value;

    if (full) {
        const parts = full.split('@');
        document.getElementById('emailId').value = parts[0];
        document.getElementById('emailDomain').value = parts[1];
    }
});
