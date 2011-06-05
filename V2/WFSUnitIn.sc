WFSUnitIn {
	
	*key { ^'wfsu' } // 'wfsu_' controls automatically become private args
	
	*firstPrivateBus { ^NumOutputBuses.ir + NumInputBuses.ir }
	
	*getControlName { |...args|
		^([this.key] ++ args).join("_").asSymbol;
	}
	
	*ar { |id = 0, numChannels = 1|
		var name = this.getControlName( 'i', 'ar', id );
		id = (name ++ "_bus").asSymbol.kr( id ); // create a control for bus
		^In.ar( this.firstPrivateBus + id, numChannels );
	}
	
	*kr { |id = 0, numChannels = 1|
		var name = this.getControlName( 'i', 'kr', id );
		id = (name ++ "_bus").asSymbol.kr( id );
		^In.kr( this.firstPrivateBus + id, numChannels );
	}
}

WFSUnitOut : WFSUnitIn {
	
	*ar { |id = 0, channelsArray|
		var name = this.getControlName( 'o', 'ar', id );
		id = (name ++ "_bus").asSymbol.kr( id );
		^Out.ar( this.firstPrivateBus + id, channelsArray );
	}
	
	*kr { |id = 0, channelsArray|
		var name = this.getControlName( 'o', 'kr', id );
		id = (name ++ "_bus").asSymbol.kr( id );
		^Out.kr( this.firstPrivateBus + id, channelsArray );
	}
}

WFSUnitMixOut : WFSUnitIn {
	
	*ar { |id = 0, channelsArray, inLevel = 0|
		var in;
		var name = this.getControlName( 'o', 'ar', id );
		id = (name ++ "_bus").asSymbol.kr( id );
		inLevel = (name ++ "_lvl").asSymbol.kr( inLevel );
		in = In.ar( this.firstPrivateBus + id, channelsArray.size ) * inLevel;
		^ReplaceOut.ar( this.firstPrivateBus + id, channelsArray + in);
	}
	
	*kr { |id = 0, channelsArray, inLevel = 0|
		var in;
		var name = this.getControlName( 'o', 'kr', id );
		id = (name ++ "_bus").asSymbol.kr( id );
		inLevel = (name ++ "_lvl").asSymbol.kr( inLevel );
		in = In.kr( this.firstPrivateBus + id, channelsArray.size ) * inLevel;
		^ReplaceOut.kr( this.firstPrivateBus + id, channelsArray + in );
	}

}