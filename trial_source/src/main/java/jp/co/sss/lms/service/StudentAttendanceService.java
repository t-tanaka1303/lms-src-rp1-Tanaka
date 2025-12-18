package jp.co.sss.lms.service;

import java.text.ParseException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jp.co.sss.lms.dto.AttendanceManagementDto;
import jp.co.sss.lms.dto.LoginUserDto;
import jp.co.sss.lms.entity.TStudentAttendance;
import jp.co.sss.lms.enums.AttendanceStatusEnum;
import jp.co.sss.lms.form.AttendanceForm;
import jp.co.sss.lms.form.DailyAttendanceForm;
import jp.co.sss.lms.mapper.TStudentAttendanceMapper;
import jp.co.sss.lms.util.AttendanceUtil;
import jp.co.sss.lms.util.Constants;
import jp.co.sss.lms.util.DateUtil;
import jp.co.sss.lms.util.LoginUserUtil;
import jp.co.sss.lms.util.MessageUtil;
import jp.co.sss.lms.util.TrainingTime;

@Service
public class StudentAttendanceService {

    @Autowired
    private DateUtil dateUtil;
    @Autowired
    private AttendanceUtil attendanceUtil;
    @Autowired
    private MessageUtil messageUtil;
    @Autowired
    private LoginUserUtil loginUserUtil;
    @Autowired
    private LoginUserDto loginUserDto;
    @Autowired
    private TStudentAttendanceMapper tStudentAttendanceMapper;

    /**
     * 勤怠管理画面用DTO取得
     */
    public List<AttendanceManagementDto> getAttendanceManagement(
            Integer courseId, Integer lmsUserId) {

        List<AttendanceManagementDto> list =
                tStudentAttendanceMapper.getAttendanceManagement(
                        courseId, lmsUserId, Constants.DB_FLG_FALSE);

        for (AttendanceManagementDto dto : list) {

            // 中抜け時間表示
            if (dto.getBlankTime() != null) {
                TrainingTime blank =
                        attendanceUtil.calcBlankTime(dto.getBlankTime());
                dto.setBlankTimeValue(blank.toString());
            }

            // ステータス表示名
            AttendanceStatusEnum statusEnum =
                    AttendanceStatusEnum.getEnum(dto.getStatus());
            if (statusEnum != null) {
                dto.setStatusDispName(statusEnum.name);
            }
        }
        return list;
    }

    /**
     * Task25：過去日勤怠未入力件数取得
     */
    public int countPastUninputAttendance(
            Integer lmsUserId, Date trainingDate) {

        List<AttendanceManagementDto> list =
                tStudentAttendanceMapper.getAttendanceManagement(
                        loginUserDto.getCourseId(),
                        lmsUserId,
                        Constants.DB_FLG_FALSE);

        int count = 0;

        LocalDate today = trainingDate.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();

        for (AttendanceManagementDto dto : list) {

            LocalDate date = dto.getTrainingDate().toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();

            // 過去日のみ
            if (!date.isBefore(today)) {
                continue;
            }

            // 欠席は除外
            if (dto.getNote() != null && dto.getNote().contains("欠席")) {
                continue;
            }

            if (isEmpty(dto.getTrainingStartTime())
                    || isEmpty(dto.getTrainingEndTime())) {
                count++;
            }
        }
        return count;
    }

    private boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * 打刻前チェック
     */
    public String punchCheck(Short attendanceType) {

        Date trainingDate = attendanceUtil.getTrainingDate();

        if (!loginUserUtil.isStudent()) {
            return messageUtil.getMessage(Constants.VALID_KEY_AUTHORIZATION);
        }

        if (!attendanceUtil.isWorkDay(
                loginUserDto.getCourseId(), trainingDate)) {
            return messageUtil.getMessage(
                    Constants.VALID_KEY_ATTENDANCE_NOTWORKDAY);
        }

        TStudentAttendance entity =
                tStudentAttendanceMapper.findByLmsUserIdAndTrainingDate(
                        loginUserDto.getLmsUserId(),
                        trainingDate,
                        Constants.DB_FLG_FALSE);

        switch (attendanceType) {
        case Constants.CODE_VAL_ATWORK:
            if (entity != null
                    && !isEmpty(entity.getTrainingStartTime())) {
                return messageUtil.getMessage(
                        Constants.VALID_KEY_ATTENDANCE_PUNCHALREADYEXISTS);
            }
            break;

        case Constants.CODE_VAL_LEAVING:
            if (entity == null
                    || isEmpty(entity.getTrainingStartTime())) {
                return messageUtil.getMessage(
                        Constants.VALID_KEY_ATTENDANCE_PUNCHINEMPTY);
            }
            if (!isEmpty(entity.getTrainingEndTime())) {
                return messageUtil.getMessage(
                        Constants.VALID_KEY_ATTENDANCE_PUNCHALREADYEXISTS);
            }

            TrainingTime start =
                    new TrainingTime(entity.getTrainingStartTime());
            TrainingTime end = new TrainingTime();
            if (start.compareTo(end) > 0) {
                return messageUtil.getMessage(
                        Constants.VALID_KEY_ATTENDANCE_TRAININGTIMERANGE);
            }
            break;
        }
        return null;
    }

    /**
     * 出勤打刻
     */
    public String setPunchIn() {

        Date now = new Date();
        Date trainingDate = attendanceUtil.getTrainingDate();

        TrainingTime startTime = new TrainingTime();
        AttendanceStatusEnum status =
                attendanceUtil.getStatus(startTime, null);

        TStudentAttendance entity =
                tStudentAttendanceMapper.findByLmsUserIdAndTrainingDate(
                        loginUserDto.getLmsUserId(),
                        trainingDate,
                        Constants.DB_FLG_FALSE);

        if (entity == null) {
            entity = new TStudentAttendance();
            entity.setLmsUserId(loginUserDto.getLmsUserId());
            entity.setTrainingDate(trainingDate);
            entity.setTrainingStartTime(startTime.toString());
            entity.setTrainingEndTime("");
            entity.setStatus(status.code);
            entity.setNote("");
            entity.setBlankTime(null);
            entity.setAccountId(loginUserDto.getAccountId());
            entity.setDeleteFlg(Constants.DB_FLG_FALSE);
            entity.setFirstCreateUser(loginUserDto.getLmsUserId());
            entity.setFirstCreateDate(now);
            entity.setLastModifiedUser(loginUserDto.getLmsUserId());
            entity.setLastModifiedDate(now);
            tStudentAttendanceMapper.insert(entity);
        } else {
            entity.setTrainingStartTime(startTime.toString());
            entity.setStatus(status.code);
            entity.setLastModifiedUser(loginUserDto.getLmsUserId());
            entity.setLastModifiedDate(now);
            tStudentAttendanceMapper.update(entity);
        }

        return messageUtil.getMessage(
                Constants.PROP_KEY_ATTENDANCE_UPDATE_NOTICE);
    }

    /**
     * 退勤打刻
     */
    public String setPunchOut() {

        Date now = new Date();
        Date trainingDate = attendanceUtil.getTrainingDate();

        TStudentAttendance entity =
                tStudentAttendanceMapper.findByLmsUserIdAndTrainingDate(
                        loginUserDto.getLmsUserId(),
                        trainingDate,
                        Constants.DB_FLG_FALSE);

        TrainingTime start =
                new TrainingTime(entity.getTrainingStartTime());
        TrainingTime end = new TrainingTime();

        AttendanceStatusEnum status =
                attendanceUtil.getStatus(start, end);

        entity.setTrainingEndTime(end.toString());
        entity.setStatus(status.code);
        entity.setLastModifiedUser(loginUserDto.getLmsUserId());
        entity.setLastModifiedDate(now);
        tStudentAttendanceMapper.update(entity);

        return messageUtil.getMessage(
                Constants.PROP_KEY_ATTENDANCE_UPDATE_NOTICE);
    }

    /**
     * 勤怠FORM生成
     */
    public AttendanceForm setAttendanceForm(
            List<AttendanceManagementDto> list) {

        AttendanceForm form = new AttendanceForm();
        form.setLmsUserId(loginUserDto.getLmsUserId());
        form.setUserName(loginUserDto.getUserName());
        form.setLeaveFlg(loginUserDto.getLeaveFlg());
        form.setBlankTimes(attendanceUtil.setBlankTime());

        for (AttendanceManagementDto dto : list) {
            DailyAttendanceForm daily = new DailyAttendanceForm();
            BeanUtils.copyProperties(dto, daily);
            daily.setTrainingDate(
                    dateUtil.toString(dto.getTrainingDate()));
            daily.setDispTrainingDate(
                    dateUtil.dateToString(
                            dto.getTrainingDate(), "yyyy年M月d日(E)"));
            form.getAttendanceList().add(daily);
        }
        return form;
    }

    /**
     * 勤怠更新
     */
    public String update(AttendanceForm attendanceForm)
            throws ParseException {

        Integer lmsUserId = loginUserUtil.isStudent()
                ? loginUserDto.getLmsUserId()
                : attendanceForm.getLmsUserId();

        Date now = new Date();

        for (DailyAttendanceForm daily
                : attendanceForm.getAttendanceList()) {

            TStudentAttendance entity;

            if (daily.getStudentAttendanceId() != null) {
                entity = tStudentAttendanceMapper.findById(
                        daily.getStudentAttendanceId());
            } else {
                entity = new TStudentAttendance();
                entity.setLmsUserId(lmsUserId);
                entity.setDeleteFlg(Constants.DB_FLG_FALSE);
                entity.setFirstCreateUser(loginUserDto.getLmsUserId());
                entity.setFirstCreateDate(now);
            }

            entity.setTrainingDate(
                    dateUtil.parse(daily.getTrainingDate()));
            entity.setTrainingStartTime(daily.getTrainingStartTime());
            entity.setTrainingEndTime(daily.getTrainingEndTime());
            entity.setBlankTime(daily.getBlankTime());
            entity.setNote(daily.getNote());
            entity.setAccountId(loginUserDto.getAccountId());
            entity.setLastModifiedUser(loginUserDto.getLmsUserId());
            entity.setLastModifiedDate(now);

            if (entity.getStudentAttendanceId() == null) {
                tStudentAttendanceMapper.insert(entity);
            } else {
                tStudentAttendanceMapper.update(entity);
            }
        }

        return messageUtil.getMessage(
                Constants.PROP_KEY_ATTENDANCE_UPDATE_NOTICE);
    }
}
