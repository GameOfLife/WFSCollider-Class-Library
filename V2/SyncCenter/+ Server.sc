+ Server {
	
	listSendSyncedBundle{ |delta = 1, msgs|
		this.listSendPosBundle( SyncCenter.getSchedulingSampleCountS(delta,this), msgs ) 
	}
	
	sendSyncedBundle{ |delta = 1 ... msgs|
		this.sendPosBundle( SyncCenter.getSchedulingSampleCountS(delta,this), *msgs ) 
	}
}