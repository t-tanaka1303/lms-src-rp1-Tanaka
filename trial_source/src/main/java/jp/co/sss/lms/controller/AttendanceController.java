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

/**
 * 勤怠管理コントローラ
 * <p>
 * 受講生および管理者が勤怠情報を参照・登録・更新するための画面制御を行う。
 * </p>
 *
 * <p>
 * 対応仕様：
 * <ul>
 *   <li>勤怠管理画面 初期表示</li>
 *   <li>出勤・退勤打刻</li>
 *   <li>過去日未入力チェック（Task.25）</li>
 *   <li>勤怠情報直接編集</li>
 * </ul>
 * </p>
 *
 * @author 東京ITスクール
 */
@Controller
@RequestMapping("/attendance")
public class AttendanceController {

    /** 勤怠サービス */
    @Autowired
    private StudentAttendanceService studentAttendanceService;

    /** ログインユーザー情報 */
    @Autowired
    private LoginUserDto loginUserDto;

    /**
     * 勤怠管理画面 初期表示
     *
     * <p>
     * ログインユーザーのコースID・LMSユーザーIDを元に勤怠管理画面用DTOリストを取得し、
     * 画面へ設定する。
     * </p>
     *
     * <p>
     * また、Task.25 として「現在日付より過去の勤怠未入力件数」を取得し、
     * 1件以上存在する場合は警告ダイアログ表示用フラグを設定する。
     * </p>
     *
     * @param model モデル
     * @return 勤怠管理画面
     */
    @RequestMapping(path = "/detail", method = RequestMethod.GET)
    public String index(Model model) {

        // 勤怠管理画面用DTOリストの取得
        List<AttendanceManagementDto> list =
                studentAttendanceService.getAttendanceManagement(
                        loginUserDto.getCourseId(),
                        loginUserDto.getLmsUserId());

        // Task.25：過去日未入力チェック
        int count =
                studentAttendanceService.countPastUninputAttendance(
                        loginUserDto.getLmsUserId(),
                        new Date());

        // 未入力が1件以上あれば警告表示
        model.addAttribute("warning", count > 0);

        model.addAttribute("attendanceManagementDtoList", list);
        return "attendance/detail";
    }

    /**
     * 勤怠管理画面 『出勤』ボタン押下
     *
     * <p>
     * 出勤登録前に妥当性チェックを行い、問題なければ出勤時刻を登録する。
     * エラーがある場合はエラーメッセージを画面へ表示する。
     * </p>
     *
     * @param model モデル
     * @return 勤怠管理画面
     */
    @RequestMapping(path = "/detail", params = "punchIn", method = RequestMethod.POST)
    public String punchIn(Model model) {

        // 出勤前チェック
        String error =
                studentAttendanceService.punchCheck(
                        Constants.CODE_VAL_ATWORK);
        model.addAttribute("error", error);

        // 出勤登録
        if (error == null) {
            model.addAttribute(
                    "message",
                    studentAttendanceService.setPunchIn());
        }

        // 勤怠一覧の再取得
        model.addAttribute(
                "attendanceManagementDtoList",
                studentAttendanceService.getAttendanceManagement(
                        loginUserDto.getCourseId(),
                        loginUserDto.getLmsUserId()));

        return "attendance/detail";
    }

    /**
     * 勤怠管理画面 『退勤』ボタン押下
     *
     * <p>
     * 退勤登録前に妥当性チェックを行い、問題なければ退勤時刻を登録する。
     * </p>
     *
     * @param model モデル
     * @return 勤怠管理画面
     */
    @RequestMapping(path = "/detail", params = "punchOut", method = RequestMethod.POST)
    public String punchOut(Model model) {

        // 退勤前チェック
        String error =
                studentAttendanceService.punchCheck(
                        Constants.CODE_VAL_LEAVING);
        model.addAttribute("error", error);

        // 退勤登録
        if (error == null) {
            model.addAttribute(
                    "message",
                    studentAttendanceService.setPunchOut());
        }

        // 勤怠一覧の再取得
        model.addAttribute(
                "attendanceManagementDtoList",
                studentAttendanceService.getAttendanceManagement(
                        loginUserDto.getCourseId(),
                        loginUserDto.getLmsUserId()));

        return "attendance/detail";
    }

    /**
     * 勤怠管理画面 『勤怠情報を直接編集する』リンク押下
     *
     * <p>
     * 勤怠情報直接変更画面へ遷移し、編集用勤怠フォームを生成する。
     * </p>
     *
     * @param model モデル
     * @return 勤怠情報直接変更画面
     */
    @RequestMapping(path = "/update")
    public String update(Model model) {

        // 勤怠管理画面用DTOリストの取得
        List<AttendanceManagementDto> list =
                studentAttendanceService.getAttendanceManagement(
                        loginUserDto.getCourseId(),
                        loginUserDto.getLmsUserId());

        // 勤怠フォームの生成
        AttendanceForm form =
                studentAttendanceService.setAttendanceForm(list);

        model.addAttribute("attendanceForm", form);
        return "attendance/update";
    }

    /**
     * 勤怠情報直接変更画面 『更新』ボタン押下
     *
     * <p>
     * 入力された勤怠情報を更新し、勤怠管理画面へ戻る。
     * </p>
     *
     * @param attendanceForm 勤怠フォーム
     * @param model モデル
     * @param result バリデーション結果
     * @return 勤怠管理画面
     * @throws Exception 日付変換エラー
     */
    @RequestMapping(path = "/update", params = "complete", method = RequestMethod.POST)
    public String complete(
            AttendanceForm attendanceForm,
            Model model,
            BindingResult result) throws Exception {

        // 更新処理
        model.addAttribute(
                "message",
                studentAttendanceService.update(attendanceForm));

        // 勤怠一覧の再取得
        model.addAttribute(
                "attendanceManagementDtoList",
                studentAttendanceService.getAttendanceManagement(
                        loginUserDto.getCourseId(),
                        loginUserDto.getLmsUserId()));

        return "attendance/detail";
    }
}
