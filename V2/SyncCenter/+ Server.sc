+ Server {
	
	listSendSyncedBundle{ |delta = 1, msgs|
		SyncCenter.listSendSyncedBundle( this, delta, msgs );
	}
	
	sendSyncedBundle{ |delta = 1 ... msgs|
		SyncCenter.sendSyncedBundle( this, delta, *msgs );
	}
}