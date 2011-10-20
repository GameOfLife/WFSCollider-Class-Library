+ WFSEvent {
	
	asUEvent {
		^wfsSynth.asUEvent
			.startTime_( startTime )
			.track_( track ); 
	}
	
}

+ WFSSynth {
	
	asUEvent { 
		var units;
		
		units = Array.new( 4 );
		
		switch( this.audioType,
			\buf, { 
				units.add( U( \bufSoundFile, [ \soundFile, this.asBufSndFile ] ) );
			}, 
			\disk, {
				units.add( U( \diskSoundFile, [ \soundFile, this.asDiskSndFile ] ) );
			},
			\blip, {
				units.add( U( \blip, args ) );
			}
		);
		
		if( [ \linear, \cubic ].includes( this.intType ) ) {
			units.add( U( \wfsPathPlayer, [ \wfsPath, WFSPathBuffer( wfsPath.asWFSPath2 ) ] ) );
		};
		
		switch( this.intType,
			\linear, {
				units.add( U( \wfsDynamicPoint, [ \pointFromBus, true ] ) );
			},
			\cubic, {
				units.add( 
					U( \wfsDynamicPoint, [ 
							\pointFromBus, true,
							\quality, \better
						] 
					) 
				);
			}, 
			\static, {
				units.add( U( \wfsStaticPoint, [ \point, wfsPath.asPoint ] ) );
			},
			\plane, {
				units.add( U( \wfsStaticPlane, [ \point, wfsPath.asPoint ] ) );
			},
			\index, {
				units.add( U( \wfsStaticIndex, [ \index, wfsPath ] ) );
			}
		);
		
		^UChain( 0, 0, dur, true, *units )
			.setGain( level.ampdb );
	}
	
	asBufSndFile { 
		^BufSndFile.newBasic( filePath, sfNumFrames, 1, sfSampleRate, startFrame, 
			startFrame + this.samplesPlayed, pbRate, loop.asInt.booleanValue 
		);
	}
	
	asDiskSndFile { 
		^BufSndFile.newBasic( filePath, sfNumFrames, 1, sfSampleRate, startFrame, 
			startFrame + this.samplesPlayed, pbRate, loop.asInt.booleanValue 
		);
	}
	
}


+ WFSScore {
	
	asUEvent {
		var uevts, maxTrack = 0;
		uevts = events.collect(_.asUEvent);
		if( uevts.size > 0 ) {
			maxTrack = uevts.collect(_.track).maxItem;
		};
		if( clickTrackPath.notNil ) {
			uevts = [ 
				UChain( 0, maxTrack + 1, inf, false,
					[ \diskSoundFile, [ \soundFile, clickTrackPath ] ],
					[ \wfsMasterOut, [ \toServers, false ] ]
				)
			] ++ uevts
		};
		^UScore( *uevts );
	}
}