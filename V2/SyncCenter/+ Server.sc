+ Server {
	
	listSendSyncedBundle{ |delta = 0.2, msgs, syncCenter|
		syncCenter = syncCenter ? SyncCenter.current;
		if( syncCenter.notNil ) {
			syncCenter.listSendSyncedBundle( this, delta, msgs );
		} {
			"falling back to normal bundle".postln;
			this.listSendBundle( delta, msgs );
		};
		
	}
	
	sendSyncedBundle{ |delta = 0.2, syncCenter ... msgs|
		syncCenter = syncCenter ? SyncCenter.current;
		if( syncCenter.notNil ) {
			syncCenter.sendSyncedBundle( this, delta, *msgs );
		} {
			"falling back to normal bundle".postln;
			this.sendBundle( delta, *msgs );
		};
	}

}