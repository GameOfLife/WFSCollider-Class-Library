LPFLag {
	
	// a lag based on LPF filter
	// switches to input at lag == 0
	// may cause artifacts at fast lagTime changes
	
	*getLag { |rate = \control, in = 0, lagTime = 0|
		var dc;
		dc = DC.perform( DC.methodSelectorForRate( rate ), in); // capture first value
		^LPF.perform( LPF.methodSelectorForRate( rate ),
			 in - dc,
			 1 / ( lagTime.max( 2 / switch( rate, 
			 		\control, {ControlRate.ir},
			 		\audio, {SampleRate.ir}
				)
			))
		) + dc;
	}
	
	*ar { |in, lagTime = 0|
		if ( lagTime.isUGen ) {
			^if( lagTime > 0, this.getLag( \audio, in, lagTime ), in );
		} {
			^if( lagTime > 0 ) { this.getLag( \audio, in, lagTime ); } { in; };
		};
	}
	
	*kr { |in, lagTime = 0|
		if ( lagTime.isUGen ) {
			^if( lagTime > 0, this.getLag( \control, in, lagTime ), in );
		} {
			^if( lagTime > 0 ) { this.getLag( \control, in, lagTime ); } { in; };
		};	
	}
}
