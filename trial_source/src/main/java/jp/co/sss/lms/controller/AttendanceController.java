package jp.co.sss.lms.controller;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
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

/**
 * 勤怠管理コントローラ
 */
@Controller
@RequestMapping("/attendance")
public class AttendanceController {

    @Autowired
    private StudentAttendanceService studentAttendanceService;
    @Autowired
    private LoginUserDto loginUserDto;

    /**
     * 勤怠管理画面 初期表示
     */
    @RequestMapping(path = "/detail", method = RequestMethod.GET)
    public String index(Model model) {

        List<AttendanceManagementDto> attendanceManagementDtoList =
                studentAttendanceService.getAttendanceManagement(
                        loginUserDto.getCourseId(),
                        loginUserDto.getLmsUserId());

        if (hasPastUninput(attendanceManagementDtoList)) {
            model.addAttribute("warning", true);
        }

        model.addAttribute("attendanceManagementDtoList", attendanceManagementDtoList);
        return "attendance/detail";
    }

    /**
     * 『出勤』ボタン押下
     */
    @RequestMapping(path = "/detail", params = "punchIn", method = RequestMethod.POST)
    public String punchIn(Model model) {

        String error = studentAttendanceService.punchCheck(Constants.CODE_VAL_ATWORK);
        model.addAttribute("error", error);

        if (error == null) {
            String message = studentAttendanceService.setPunchIn();
            model.addAttribute("message", message);
        }

        model.addAttribute("attendanceManagementDtoList",
                studentAttendanceService.getAttendanceManagement(
                        loginUserDto.getCourseId(),
                        loginUserDto.getLmsUserId()));

        return "attendance/detail";
    }

    /**
     * 『退勤』ボタン押下
     */
    @RequestMapping(path = "/detail", params = "punchOut", method = RequestMethod.POST)
    public String punchOut(Model model) {

        String error = studentAttendanceService.punchCheck(Constants.CODE_VAL_LEAVING);
        model.addAttribute("error", error);

        if (error == null) {
            String message = studentAttendanceService.setPunchOut();
            model.addAttribute("message", message);
        }

        model.addAttribute("attendanceManagementDtoList",
                studentAttendanceService.getAttendanceManagement(
                        loginUserDto.getCourseId(),
                        loginUserDto.getLmsUserId()));

        return "attendance/detail";
    }

    /**
     * 『勤怠情報を直接編集する』リンク押下
     */
    @RequestMapping(path = "/update")
    public String update(Model model) {

        List<AttendanceManagementDto> list =
                studentAttendanceService.getAttendanceManagement(
                        loginUserDto.getCourseId(),
                        loginUserDto.getLmsUserId());

        AttendanceForm attendanceForm =
                studentAttendanceService.setAttendanceForm(list);

        model.addAttribute("attendanceForm", attendanceForm);
        return "attendance/update";
    }

    /**
     * 勤怠情報直接変更画面 『更新』ボタン押下
     * 
     * @param attendanceForm
     * @param model
     * @param result
     * @return 勤怠管理画面
     * @throws ParseException
     */
    @RequestMapping(path = "/update", params = "complete", method = RequestMethod.POST)
    public String complete(AttendanceForm attendanceForm, Model model, BindingResult result)
            throws ParseException {

        // 勤怠情報更新（ParseException対応）
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        for (var dailyForm : attendanceForm.getAttendanceList()) {
            if (dailyForm.getTrainingDate() != null) {
                // 日付文字列をパースしてDate型に変換（SimpleDateFormat使用）
                sdf.parse(dailyForm.getTrainingDate());
            }
        }

        String message = studentAttendanceService.update(attendanceForm);
        model.addAttribute("message", message);

        // 更新後の勤怠一覧再取得
        List<AttendanceManagementDto> attendanceManagementDtoList =
                studentAttendanceService.getAttendanceManagement(
                        loginUserDto.getCourseId(),
                        loginUserDto.getLmsUserId());
        model.addAttribute("attendanceManagementDtoList", attendanceManagementDtoList);

        return "attendance/detail";
    }

    /**
     * 過去日の未入力チェック
     */
    private boolean hasPastUninput(List<AttendanceManagementDto> list) {

        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        LocalTime endLimit = LocalTime.of(18, 0);

        for (AttendanceManagementDto dto : list) {

            LocalDate date = dto.getTrainingDate()
                    .toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();

            if (isAbsent(dto)) {
                continue;
            }

            String start = dto.getTrainingStartTime();
            String end   = dto.getTrainingEndTime();

            if (date.isBefore(today)) {
                if (isEmpty(start) || isEmpty(end)) {
                    return true;
                }
                continue;
            }

            if (Boolean.TRUE.equals(dto.getIsToday())) {
                if (isEmpty(start)) {
                    return true;
                }
                if (now.isAfter(endLimit) && isEmpty(end)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * 欠席判定
     */
    private boolean isAbsent(AttendanceManagementDto dto) {
        return "欠席".equals(dto.getStatusDispName());
    }

    /**
     * 空文字判定
     */
    private boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
}
