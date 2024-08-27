package com.finalproject.sulbao.member.controller;

import com.finalproject.sulbao.login.model.service.LoginService;
import com.finalproject.sulbao.member.dto.MemberDto;
import com.finalproject.sulbao.product.model.dto.ProductDTO;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@RequestMapping("/member")
@Controller
@Slf4j
public class MemberController {

    private final LoginService service;
    public MemberController(LoginService service) {
        this.service = service;
    }

    @GetMapping("/memberList")
    public String memberList(Model model) {

        List<MemberDto> memberList = service.findMemberList();
        model.addAttribute("memberList", memberList);

        return "admin/member/memberList";
    }

    @GetMapping("/proList")
    public String proMemberList(Model model){

        List<MemberDto> memberList = service.findProMemberList();
        model.addAttribute("memberList", memberList);

        return "admin/member/proMemberList";
    }

    @GetMapping("/sellerList")
    public String sellerList(Model model){

        List<MemberDto> sellerList = service.findSellerList();
        model.addAttribute("sellerList", sellerList);
        return "admin/member/sellerList";
    }

    // 멤버 활성 상태 변경
    @PutMapping("/updateEnable")
    @ResponseBody
    public String updateEnable(String memberList, String type, String available) {

        if(memberList == null || memberList.isEmpty()){
            return "fail";
        }
        service.updateEnable(memberList,type,available);
        return "success";
    }


    // 전문가 승인
    @PutMapping("/updateProStatus")
    @ResponseBody
    public String updateProStatus(String memberList, String type, String proStatus) {
        if(memberList == null || memberList.isEmpty()){
            return "fail";
        }
        service.updateProStatus(memberList,type,proStatus);
        return "success";
    }

    // 판매자 승인
    @PutMapping("/updateSellStatus")
    @ResponseBody
    public String updateSellStatus(String sellerList, String type, String sellStatus) {
        if(sellerList == null || sellerList.isEmpty()){
            return "fail";
        }
        service.updateSellStatus(sellerList,type,sellStatus);
        return "success";
    }
}


