+ Server {
	
	listSendSyncedBundle{ |delta = 1, msgs, syncCenter|
		syncCenter = syncCenter ? SyncCenter.current;
		syncCenter.listSendSyncedBundle( this, delta, msgs );
	}
	
	sendSyncedBundle{ |delta = 1, syncCenter ... msgs|
		syncCenter = syncCenter ? SyncCenter.current;
		syncCenter.sendSyncedBundle( this, delta, *msgs );
	}

}