package com.ozy.seckill.service;

import org.springframework.stereotype.Service;


public interface SMSService {
	/**
	 * @param phoneNum
	 * @param msg payurl
	 * @return
	 */
	void sendSuccessMessage(String phoneNum,String msg);

	/**
	 * @param phoneNum
	 * @param msg 失败原因
	 * @return
	 */
	void sendFailMessage(String phoneNum,String msg);
}
