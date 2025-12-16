package jp.co.sss.lms.service;

import java.text.ParseException;
import java.util.ArrayList;
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

    public List<AttendanceManagementDto> getAttendanceManagement(
            Integer courseId, Integer lmsUserId) {

        List<AttendanceManagementDto> list =
                tStudentAttendanceMapper.getAttendanceManagement(
                        courseId, lmsUserId, Constants.DB_FLG_FALSE);

        for (AttendanceManagementDto dto : list) {
            if (dto.getBlankTime() != null) {
                TrainingTime blankTime =
                        attendanceUtil.calcBlankTime(dto.getBlankTime());
                dto.setBlankTimeValue(String.valueOf(blankTime));
            }
            AttendanceStatusEnum statusEnum =
                    AttendanceStatusEnum.getEnum(dto.getStatus());
            if (statusEnum != null) {
                dto.setStatusDispName(statusEnum.name);
            }
        }
        return list;
    }

    // タスク25：過去日勤怠未入力件数取得
    public int countPastUninputAttendance(Integer lmsUserId, Date trainingDate) {
        return tStudentAttendanceMapper.countPastUninputAttendance(
                lmsUserId,
                Constants.DB_FLG_FALSE,
                trainingDate
        );
    }

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
                    && !"".equals(entity.getTrainingStartTime())) {
                return messageUtil.getMessage(
                        Constants.VALID_KEY_ATTENDANCE_PUNCHALREADYEXISTS);
            }
            break;

        case Constants.CODE_VAL_LEAVING:
            if (entity == null
                    || "".equals(entity.getTrainingStartTime())) {
                return messageUtil.getMessage(
                        Constants.VALID_KEY_ATTENDANCE_PUNCHINEMPTY);
            }
            if (!"".equals(entity.getTrainingEndTime())) {
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
            entity.setAccountId(loginUserDto.getAccountId());
            entity.setDeleteFlg(Constants.DB_FLG_FALSE);
            entity.setFirstCreateUser(loginUserDto.getLmsUserId());
            entity.setFirstCreateDate(now);
            entity.setLastModifiedUser(loginUserDto.getLmsUserId());
            entity.setLastModifiedDate(now);
            entity.setBlankTime(null);
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

    public AttendanceForm setAttendanceForm(
            List<AttendanceManagementDto> list) {

        AttendanceForm form = new AttendanceForm();
        form.setAttendanceList(new ArrayList<>());
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

    public String update(AttendanceForm attendanceForm)
            throws ParseException {

        Integer lmsUserId = loginUserUtil.isStudent()
                ? loginUserDto.getLmsUserId()
                : attendanceForm.getLmsUserId();

        List<TStudentAttendance> entityList =
                tStudentAttendanceMapper.findByLmsUserId(
                        lmsUserId, Constants.DB_FLG_FALSE);

        Date now = new Date();

        for (DailyAttendanceForm daily : attendanceForm.getAttendanceList()) {

            TStudentAttendance entity = new TStudentAttendance();
            BeanUtils.copyProperties(daily, entity);
            entity.setTrainingDate(
                    dateUtil.parse(daily.getTrainingDate()));
            entity.setLmsUserId(lmsUserId);
            entity.setAccountId(loginUserDto.getAccountId());
            entity.setLastModifiedUser(loginUserDto.getLmsUserId());
            entity.setLastModifiedDate(now);
            entity.setDeleteFlg(Constants.DB_FLG_FALSE);

            entityList.add(entity);
        }

        for (TStudentAttendance entity : entityList) {
            if (entity.getStudentAttendanceId() == null) {
                entity.setFirstCreateUser(loginUserDto.getLmsUserId());
                entity.setFirstCreateDate(now);
                tStudentAttendanceMapper.insert(entity);
            } else {
                tStudentAttendanceMapper.update(entity);
            }
        }

        return messageUtil.getMessage(
                Constants.PROP_KEY_ATTENDANCE_UPDATE_NOTICE);
    }
}
