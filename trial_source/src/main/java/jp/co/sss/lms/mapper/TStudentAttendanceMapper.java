package jp.co.sss.lms.mapper;

import java.util.Date;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import jp.co.sss.lms.dto.AttendanceManagementDto;
import jp.co.sss.lms.entity.TStudentAttendance;

/**
 * 勤怠情報（受講生入力）テーブルマッパー
 */
@Mapper
public interface TStudentAttendanceMapper {

    /**
     * 勤怠情報（受講生入力）取得（LMSユーザーID）
     */
    List<TStudentAttendance> findByLmsUserId(
            @Param("lmsUserId") Integer lmsUserId,
            @Param("deleteFlg") Short deleteFlg);

    /**
     * 勤怠情報（受講生入力）取得（LMSユーザーID＆日付）
     */
    TStudentAttendance findByLmsUserIdAndTrainingDate(
            @Param("lmsUserId") Integer lmsUserId,
            @Param("trainingDate") Date trainingDate,
            @Param("deleteFlg") Short deleteFlg);

    /**
     * 勤怠管理画面用DTOリスト取得
     */
    List<AttendanceManagementDto> getAttendanceManagement(
            @Param("courseId") Integer courseId,
            @Param("lmsUserId") Integer lmsUserId,
            @Param("deleteFlg") Short deleteFlg);

    /**
     * タスク25
     * 過去日勤怠未入力件数取得
     */
    int countPastUninputAttendance(
    	    @Param("lmsUserId") Integer lmsUserId,
    	    @Param("trainingDate") Date trainingDate,
    	    @Param("deleteFlg") Short deleteFlg
    	);

    /**
     * 勤怠情報（受講生入力）登録
     */
    Boolean insert(TStudentAttendance tStudentAttendance);

    /**
     * 勤怠情報（受講生入力）更新
     */
    Boolean update(TStudentAttendance tStudentAttendance);

    /**
     * 勤怠情報（受講生入力）取得（主キー）
     */
    TStudentAttendance findById(
            @Param("studentAttendanceId") Integer studentAttendanceId);
}
