/*


UIn -> *ar { |id, numChannels|
    id (0): corresponds with default private bus to read from
    numChannels (1): number of buses

     -> creates an audio input from a private bus
     -> generates the following control(s) for the synth:
     	\u_i_ar_<id>_bus (id)
     -> if numChannels > 1 it creates multiple inputs with adjecent id's
     	
  *kr { |id, numChannels|
    id (0): corresponds with default private bus to send to
    numChannels (1): number of buses

     -> creates a control input from a private bus
     -> generates the following control(s) for the synth:
     	\u_i_kr_<id>_bus (id)
     
UOut -> *ar { |id, channelsArray|
    id: corresponds with default private bus to send to
    channelsArray: array of input channels

     -> creates audio outputs for each of the channels to a private bus
     -> generates the following control(s) for the synth:
     	\u_o_ar_<id>_bus (id)	
    
  *kr { |id, channelsArray|
    id: corresponds with default private bus to send to
	channelsArray: array of input channels
	
     -> creates a control outputs for each of the channels to a private bus
     -> generates the following control(s) for the synth:
     	\u_o_kr_<id>_bus (id)
     
    
*/

UIn {
	
	*key { ^'u' } // 'wfsu_' controls automatically become private args
	
	*firstPrivateBus { ^NumOutputBuses.ir + NumInputBuses.ir }
	*firstControlBus { ^1000 } // don't use any buses below this value
	
	*firstBusFor { |mode = \ar| 
		switch( mode,
			\ar, { ^this.firstPrivateBus },
			\kr, { ^this.firstControlBus }
		);
	}
	
	*getControlName { |...args|
		^([this.key] ++ args).join("_").asSymbol;
	}
	
	*getControl { |mode = \kr, name, what, value, lag|
		^(name ++ "_" ++ what).asSymbol.perform( mode, value, lag );
	}
	
	*ar { |id = 0, numChannels = 1|
		^numChannels.collect({ |i| this.new1( \ar, id + i ); }).returnFirstIfSize1;
	}
	
	*kr { |id = 0, numChannels = 1|
		^numChannels.collect({ |i| this.new1( \kr, id + i ); }).returnFirstIfSize1;
	}	
	
	*new1 { |selector = \ar, id = 0|
		id = this.getControl( \kr, this.getControlName( 'i',  selector, id ), "bus", id); 
		^In.perform( selector, this.firstBusFor( selector ) + id, 1 );
	}
}

UOut : UIn { // overwrites bus (ReplaceOut)
	
	*ar { |id = 0, channelsArray, offset = false| // use offset = true for generators (sample-sync)
		^channelsArray.asCollection.collect({ |item, i| 
			this.new1( \ar, id + i, item, offset ) 
		});
	}
	
	*kr { |id = 0, channelsArray|
		^channelsArray.asCollection.collect({ |item, i| 
			this.new1( \kr, id + i, item ) 
		});
	}
	
	*new1 { |selector = \ar, id = 0, input, offset = false|
		id = this.getControl( \kr, this.getControlName( 'o', selector, id ), "bus", id); 
		if( offset ) {
			ReplaceOut.perform( selector, this.firstBusFor( selector ) + id, Silent.ar );
			^OffsetOut.perform( selector, this.firstBusFor( selector ) + id, input );
		} {
			^ReplaceOut.perform( selector, this.firstBusFor( selector ) + id, input );
		};
	}
}

UMixOut : UIn {
	
	*ar { |id = 0, channelsArray, inLevel = 0, offset = false |
		^channelsArray.asCollection.collect({ |item, i| 
			this.new1( \ar, id + i, item, inLevel, offset ) 
		});
	}
	
	*kr { |id = 0, channelsArray, inLevel = 0|
		^channelsArray.asCollection.collect({ |item, i| 
			this.new1( \kr, id + i, item, inLevel ) 
		});
	}
	
	*new1 { |selector = \ar, id = 0, input, inLevel=0, offset = false|
		var in, out;
		var name = this.getControlName( 'o', selector, id );
		id = this.getControl( \kr, name, "bus", id);
		inLevel = this.getControl( \kr, name, "lvl", inLevel);
		in = In.perform( selector, this.firstBusFor( selector ) + id, 1) * inLevel;
		if( offset ) {
			ReplaceOut.perform( selector, this.firstBusFor( selector ) + id, in );
			^OffsetOut.perform( selector, this.firstBusFor( selector ) + id, input );
		} {
			^ReplaceOut.perform( selector, this.firstBusFor( selector ) + id, input + in );
		};
	}

}

+ Collection {
	returnFirstIfSize1 { ^if( this.size == 1 ) { this[0] } { this } }
}