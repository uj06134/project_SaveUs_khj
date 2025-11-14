package com.example.Ex02.controller;

import com.example.Ex02.dto.UserJoinDto;
import com.example.Ex02.dto.UserLoginDto;
import com.example.Ex02.mapper.UserMapper;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class UserController {

    @Autowired
    private UserMapper userMapper;

    // 회원가입 페이지 이동
    @GetMapping("/join")
    public String joinPage(@ModelAttribute("userDto") UserJoinDto userJoinDto) {
        return "user/userInsert";
    }

    // 회원가입 처리
    @PostMapping("/user/insert")
    public String insertUser(
            @Valid @ModelAttribute("userDto") UserJoinDto userJoinDto,
            BindingResult bindingResult,
            HttpSession session) {

        if (bindingResult.hasErrors()) {
            return "user/userInsert";
        }

        // DB에 INSERT (userId는 MyBatis에서 keyProperty로 채워져야 함)
        userMapper.insertUser(userJoinDto);

        // userId가 정상적으로 들어왔는지 확인
        System.out.println("회원가입 후 생성된 userId = " + userJoinDto.getUserId());

        // 세션 저장
        session.setAttribute("userId", userJoinDto.getUserId());
        session.setAttribute("loginUser", userJoinDto);

        return "redirect:/login";  // 필요하면 survey로 변경
    }

    // 이메일 중복확인
    @GetMapping("/user/checkEmail")
    @ResponseBody
    public String checkEmail(@RequestParam("email") String email) {
        int cnt = userMapper.countByEmail(email);
        return cnt > 0 ? "duplicate" : "ok";
    }

    // 로그인 페이지
    @GetMapping("/login")
    public String loginForm(Model model) {
        model.addAttribute("userLoginDto", new UserLoginDto());
        return "user/userLogin";
    }

    // 로그인 처리
    @PostMapping("/login/proc")
    public String doLogin(
            @ModelAttribute("userLoginDto") UserLoginDto userLoginDto,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        // 로그인 DTO의 이메일로 조회해야 함
        UserJoinDto user = userMapper.findByEmail(userLoginDto.getEmail());
        System.out.println("조회된 유저: " + user);

        // 아이디 없음
        if (user == null) {
            redirectAttributes.addFlashAttribute("loginError", "이메일 또는 비밀번호가 일치하지 않습니다.");
            return "redirect:/login";
        }

        // 비밀번호 불일치
        if (!user.getPassword().equals(userLoginDto.getPassword())) {
            redirectAttributes.addFlashAttribute("loginError", "이메일 또는 비밀번호가 일치하지 않습니다.");
            return "redirect:/login";
        }

        // 로그인 성공 → 세션 저장
        session.setAttribute("userId", user.getUserId());
        session.setAttribute("loginUser", user);

        return "redirect:/my-page";
    }

    // 마이페이지
    @GetMapping("/my-page")
    public String myPage(HttpSession session, Model model) {

        Long userId = (Long) session.getAttribute("userId");

        // 로그인 안 되어 있을 때
        if (userId == null) {
            return "redirect:/login";
        }

        // 사용자 정보 조회
        UserJoinDto user = userMapper.findById(userId);
        model.addAttribute("user", user);

        return "user/myPage";
    }
}
