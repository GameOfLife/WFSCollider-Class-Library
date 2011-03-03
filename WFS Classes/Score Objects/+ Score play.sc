+ WFSScore {
	
	play2{ |pos|
		this.asFlatScore( excludeMuted: true ).playFlatSync( nil, pos );				
	} 
	
	playFlatSync2 { |wfsServers, startTimeOffset = 0|
		// only for sorted and flattened scores
		// assumes that the sync  pulses will start 
		var playEvents;
		wfsServers = wfsServers ? WFSServers.default;
		
		this.trimEventsFromStart( startTimeOffset );
	
		playEvents = events
			.select( _.muted.not ) // exclude muted
			.select( _.startTime >= startTimeOffset ); // select after startTimeOffset
		
		^Task({
			var currentTime;
			currentTime = startTimeOffset;
			playEvents.do({ |event|
				var delta;
				delta = (event.startTime - currentTime);
				delta.wait;
				event.wfsSynth.playNow( wfsServers, event.startTime );
				currentTime = currentTime + delta;
				});
		}, WFSSynth.clock).start;
		
	}
}
