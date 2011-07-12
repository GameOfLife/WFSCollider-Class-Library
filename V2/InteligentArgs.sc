

EQArg {
    var <>values; // i.e. [[100,0,1], [250,0,1], [1000,0,1], [3500,0,1], [6000,0,1]]

    *new{ |values|^super.newCopyArgs(values) }

	asControlInputFor { ^this.toSynthArg }

	toSynthArg{ ^values.collect({ |item,i|
	    [ item[0].cpsmidi - 1000.cpsmidi, item[1], item[2].log10 ] }).flat
    }

	printOn { arg stream;
		stream << "a " << this.class.name << "(" <<*[
		    values]
		<<")"
	}

    storeOn { arg stream;
		stream << this.class.name << "(" <<*[
		    values]
		<<")"
	}
}

/*

Udef(\noiseEnv,{ Out.ar(0,WhiteNoise.ar*0.2*EventEnv.ar)  })
U(\noiseEnv,[\eventEnv,EventEnv(1,1,1)]).prepareAndStart(s)

*/
EventEnv {
    var duration, fadeIn, fadeOut;

    *new{ |duration, fadeIn, fadeOut|
        ^super.newCopyArgs(duration, fadeIn, fadeOut)
    }

    *kr{
        var duration, fadeInTime, fadeOutTime;
        #duration, fadeInTime, fadeOutTime = \eventEnv.ir([1,10,1]);
        ^EnvGen.kr( Env.new([0,1,1,0],[ fadeInTime,(duration - (fadeInTime + fadeOutTime)).max(0),fadeOutTime]), doneAction:14);
    }

    *ar{
        var duration, fadeInTime, fadeOutTime;
        #duration, fadeInTime, fadeOutTime = \eventEnv.ir([1,10,1]);
        ^EnvGen.ar( Env.new([0,1,1,0],[ fadeInTime,(duration - (fadeInTime + fadeOutTime)).max(0),fadeOutTime]), doneAction:14);
    }

    asControlInputFor { ^[duration, fadeIn, fadeOut] }

    printOn { arg stream;
		stream << "a " << this.class.name << "(" <<*[
		    duration, fadeIn, fadeOut]
		<<")"
	}

    storeOn { arg stream;
		stream << this.class.name << "(" <<*[
		    duration, fadeIn, fadeOut]
		<<")"
	}
}