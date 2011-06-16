+ Server {
	
	listSendSyncedBundle{ |delta = 1, msgs, syncCenter|
		syncCenter = syncCenter ? SyncCenter;
		syncCenter.listSendSyncedBundle( this, delta, msgs );
	}
	
	sendSyncedBundle{ |delta = 1, syncCenter ... msgs|
		syncCenter = syncCenter ? SyncCenter;
		syncCenter.sendSyncedBundle( this, delta, *msgs );
	}

}