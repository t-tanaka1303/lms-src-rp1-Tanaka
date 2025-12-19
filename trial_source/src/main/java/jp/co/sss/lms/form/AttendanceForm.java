package jp.co.sss.lms.form;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import lombok.Data;

/**
 * 勤怠フォーム
 * 
 * <p>
 * 日次の勤怠情報を保持し、勤怠情報直接編集画面で使用するフォーム。
 * </p>
 * 
 * @author 東京ITスクール
 */
@Data
public class AttendanceForm {

    /** LMSユーザーID */
    private Integer lmsUserId;
    /** グループID */
    private Integer groupId;
    /** 年間計画No */
    private String nenkanKeikakuNo;
    /** ユーザー名 */
    private String userName;
    /** 退校フラグ */
    private Integer leaveFlg;
    /** 退校日 */
    private String leaveDate;
    /** 退校日（表示用） */
    private String dispLeaveDate;

    /** 中抜け時間(プルダウン) */
    private LinkedHashMap<Integer, String> blankTimes;

    /** 日次の勤怠フォームリスト */
    private List<DailyAttendanceForm> attendanceList;

    // ==================================================
    // Task26: 出勤・退勤時間を「時・分」に分けて扱うための選択肢
    // ==================================================

    /** 出勤・退勤時間（時）プルダウン用マップ（0～23） */
    private LinkedHashMap<Integer, String> hourMap;

    /** 出勤・退勤時間（分）プルダウン用マップ（0～59） */
    private LinkedHashMap<Integer, String> minuteMap;

    /**
     * コンストラクタ
     * <p>
     * リスト・マップ類を初期化することで、nullによる例外を防止する
     * </p>
     */
    public AttendanceForm() {
        this.attendanceList = new ArrayList<>();
        this.blankTimes = new LinkedHashMap<>();
        this.hourMap = new LinkedHashMap<>();
        this.minuteMap = new LinkedHashMap<>();
    }
}
