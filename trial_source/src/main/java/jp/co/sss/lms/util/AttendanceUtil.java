package jp.co.sss.lms.util;

import java.text.ParseException;
import java.util.Date;
import java.util.LinkedHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jp.co.sss.lms.enums.AttendanceStatusEnum;
import jp.co.sss.lms.mapper.MSectionMapper;

/**
 * 勤怠管理のユーティリティクラス
 * 
 * @author 東京ITスクール
 */
@Component
public class AttendanceUtil {

	@Autowired
	private DateUtil dateUtil;
	@Autowired
	private MSectionMapper mSectionMapper;

	/**
	 * SSS定時・出退勤時間を元に、遅刻早退を判定をする
	 */
	public AttendanceStatusEnum getStatus(
			TrainingTime trainingStartTime,
			TrainingTime trainingEndTime) {
		return getStatus(
				trainingStartTime,
				trainingEndTime,
				Constants.SSS_WORK_START_TIME,
				Constants.SSS_WORK_END_TIME);
	}

	private AttendanceStatusEnum getStatus(
			TrainingTime trainingStartTime,
			TrainingTime trainingEndTime,
			TrainingTime workStartTime,
			TrainingTime workEndTime) {

		if (workStartTime == null || workStartTime.isBlank()
				|| workEndTime == null || workEndTime.isBlank()) {
			return AttendanceStatusEnum.NONE;
		}

		boolean isLate = false, isEarly = false;

		if (trainingStartTime != null && trainingStartTime.isNotBlank()) {
			isLate = (trainingStartTime.compareTo(workStartTime) > 0);
		}
		if (trainingEndTime != null && trainingEndTime.isNotBlank()) {
			isEarly = (trainingEndTime.compareTo(workEndTime) < 0);
		}

		if (isLate && isEarly) {
			return AttendanceStatusEnum.TARDY_AND_LEAVING_EARLY;
		}
		if (isLate) {
			return AttendanceStatusEnum.TARDY;
		}
		if (isEarly) {
			return AttendanceStatusEnum.LEAVING_EARLY;
		}
		return AttendanceStatusEnum.NONE;
	}

	/**
	 * 中抜け時間を時(hour)と分(minute)に変換
	 */
	public TrainingTime calcBlankTime(int min) {
		int hour = min / 60;
		int minute = min % 60;
		return new TrainingTime(hour, minute);
	}

	/**
	 * 時刻分を丸めた本日日付を取得
	 */
	public Date getTrainingDate() {
		try {
			return dateUtil.parse(dateUtil.toString(new Date()));
		} catch (ParseException e) {
			throw new IllegalStateException();
		}
	}

	/**
	 * 休憩時間取得
	 */
	public LinkedHashMap<Integer, String> setBlankTime() {
		LinkedHashMap<Integer, String> map = new LinkedHashMap<>();
		map.put(null, "");

		for (int i = 15; i < 480; i += 15) {
			int hour = i / 60;
			int minute = i % 60;

			String time;
			if (hour == 0) {
				time = minute + "分";
			} else if (minute == 0) {
				time = hour + "時間";
			} else {
				time = hour + "時" + minute + "分";
			}

			map.put(i, time);
		}
		return map;
	}

	/**
	 * 研修日の判定
	 */
	public boolean isWorkDay(Integer courseId, Date trainingDate) {
		Integer count =
				mSectionMapper.getSectionCountByCourseId(courseId, trainingDate);
		return count > 0;
	}

	// ==================================================
	// task26: 出勤・退勤時間「時・分」プルダウン用
	// ==================================================

	/**
	 * 時プルダウン用マップ（0～23）
	 */
	public LinkedHashMap<Integer, String> getHourMap() {
		LinkedHashMap<Integer, String> map = new LinkedHashMap<>();
		for (int i = 0; i <= 23; i++) {
			map.put(i, String.format("%02d", i));
		}
		return map;
	}

	/**
	 * 分プルダウン用マップ（0～59）
	 */
	public LinkedHashMap<Integer, String> getMinuteMap() {
		LinkedHashMap<Integer, String> map = new LinkedHashMap<>();
		for (int i = 0; i <= 59; i++) {
			map.put(i, String.format("%02d", i));
		}
		return map;
	}
}
