package jp.co.sss.lms.controller;

import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import jp.co.sss.lms.dto.AttendanceManagementDto;
import jp.co.sss.lms.dto.LoginUserDto;
import jp.co.sss.lms.form.AttendanceForm;
import jp.co.sss.lms.service.StudentAttendanceService;
import jp.co.sss.lms.util.Constants;

@Controller
@RequestMapping("/attendance")
public class AttendanceController {

    @Autowired
    private StudentAttendanceService studentAttendanceService;
    @Autowired
    private LoginUserDto loginUserDto;

    @RequestMapping(path = "/detail", method = RequestMethod.GET)
    public String index(Model model) {

        List<AttendanceManagementDto> list =
                studentAttendanceService.getAttendanceManagement(
                        loginUserDto.getCourseId(),
                        loginUserDto.getLmsUserId());

        // タスク25：過去日未入力チェック
        int count =
                studentAttendanceService.countPastUninputAttendance(
                        loginUserDto.getLmsUserId(),
                        new Date());
        model.addAttribute("warning", count > 0);

        model.addAttribute("attendanceManagementDtoList", list);
        return "attendance/detail";
    }

    @RequestMapping(path = "/detail",params = "punchIn",method = RequestMethod.POST)
    public String punchIn(Model model) {

        String error =
                studentAttendanceService.punchCheck(
                        Constants.CODE_VAL_ATWORK);
        model.addAttribute("error", error);

        if (error == null) {
            model.addAttribute(
                    "message",
                    studentAttendanceService.setPunchIn());
        }

        model.addAttribute(
                "attendanceManagementDtoList",
                studentAttendanceService.getAttendanceManagement(
                        loginUserDto.getCourseId(),
                        loginUserDto.getLmsUserId()));

        return "attendance/detail";
    }

    @RequestMapping(path = "/detail", params = "punchOut", method = RequestMethod.POST)
    public String punchOut(Model model) {

        String error =
                studentAttendanceService.punchCheck(
                        Constants.CODE_VAL_LEAVING);
        model.addAttribute("error", error);

        if (error == null) {
            model.addAttribute(
                    "message",
                    studentAttendanceService.setPunchOut());
        }

        model.addAttribute(
                "attendanceManagementDtoList",
                studentAttendanceService.getAttendanceManagement(
                        loginUserDto.getCourseId(),
                        loginUserDto.getLmsUserId()));

        return "attendance/detail";
    }

    @RequestMapping(path = "/update")
    public String update(Model model) {

        List<AttendanceManagementDto> list =
                studentAttendanceService.getAttendanceManagement(
                        loginUserDto.getCourseId(),
                        loginUserDto.getLmsUserId());

        AttendanceForm form =
                studentAttendanceService.setAttendanceForm(list);

        model.addAttribute("attendanceForm", form);
        return "attendance/update";
    }

    @RequestMapping(path = "/update", params = "complete", method = RequestMethod.POST)
    public String complete(
            AttendanceForm attendanceForm,
            Model model,
            BindingResult result) throws Exception {

        model.addAttribute(
                "message",
                studentAttendanceService.update(attendanceForm));

        model.addAttribute(
                "attendanceManagementDtoList",
                studentAttendanceService.getAttendanceManagement(
                        loginUserDto.getCourseId(),
                        loginUserDto.getLmsUserId()));

        return "attendance/detail";
    }
}
