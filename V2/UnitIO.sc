AbstractUIO {

	*key { ^'u' } // 'u_' controls automatically become private args

	*firstPrivateBus { ^NumOutputBuses.ir + NumInputBuses.ir }

	*getControlName { |...args|
		^([this.key] ++ args).join("_").asSymbol;
	}

}

UIn : AbstractUIO {

	
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

UOut : AbstractUIO {

    *crossfadeEnv { ^Env.new([1,1,0],[1,1],'linear',1) }

    *ar { |id = 0, channelsArray, xfade = true|
        var env, name = this.getControlName( 'o', 'ar', id );
        if (xfade) {
            env =  EnvGen.ar(this.crossfadeEnv, \u_gate.kr(1), timeScale: \u_timeScale.kr(1) , doneAction:1 );
		    id = (name ++ "_bus").asSymbol.kr( id );
		    XOut.ar( this.firstPrivateBus + id, env, channelsArray );
		} {
		    ReplaceOut.ar( this.firstPrivateBus + id, channelsArray )
		}
	}

	*kr { |id = 0, channelsArray, xfade = true|
		var env, name = this.getControlName( 'o', 'kr', id );
		^if(xfade) {
		    env =  EnvGen.kr(this.crossfadeEnv, \u_gate.kr(1), timeScale: \u_timeScale.kr(1) , doneAction:1);
		    id = (name ++ "_bus").asSymbol.kr( id );
		    XOut.kr( this.firstPrivateBus + id, env, channelsArray );
		} {
            ReplaceOut.kr( this.firstPrivateBus + id, channelsArray )
		}
	}
}

UMixOut : AbstractUIO {

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
