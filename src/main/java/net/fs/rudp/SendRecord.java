// Copyright (c) 2015 D1SM.net

package net.fs.rudp;

public class SendRecord {
	
	private int sendSize;
	private int	sendSize_First;
	
	private int sendCount;
	
	int ackedSize;
	
	int timeId;
	
	private int speed;
	
	private boolean speedRecored=false;
	
	private int reSended;
	
	SendRecord(){
		
	}
	
	float getDropRate(){
		int droped=getSendSize()-getAckedSize();
		if(droped<0){
			droped=0;
		}
		float dropRate=0;
		if(getSendSize()>0){
			dropRate=(float)droped/getSendSize();
		}
		return dropRate;
	}
	
	float getResendRate(){
		float resendRate=0;
		if(getSendSize_First()>0){
			resendRate=(float) getReSended()/getSendSize_First();
		}
		return resendRate;
	}
	
	void addResended(int size){
		reSended +=size;
	}
	
	void addSended(int size){
		sendCount++;
		sendSize+=size;
	}
	
	void addSended_First(int size){
		sendSize_First+=size;
	}

	public int getSendSize() {
		return sendSize;
	}

	public int getSendCount() {
		return sendCount;
	}

	public int getAckedSize() {
		return ackedSize;
	}

	//接收到的数据大小
	public void setAckedSize(int ackedSize) {
		if(ackedSize>this.ackedSize){
			this.ackedSize = ackedSize;
		}
	}

	public int getTimeId() {
		return timeId;
	}

	public void setTimeId(int timeId) {
		this.timeId = timeId;
	}

	public int getSpeed() {
		return speed;
	}

	public void setSpeed(int speed) {
		this.speed = speed;
		speedRecored=true;
	}
	
	public boolean isSpeedRecored(){
		return speedRecored;
	}

	public int getReSended() {
		return reSended;
	}

	public void setReSended(int reSended) {
		this.reSended = reSended;
	}

	public int getSendSize_First() {
		return sendSize_First;
	}

	public void setSendSize_First(int sendSize_First) {
		this.sendSize_First = sendSize_First;
	}

}
