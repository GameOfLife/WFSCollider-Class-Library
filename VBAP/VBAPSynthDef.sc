VBAPSynthDef {
     /*
     Simple - The virtual point can only move in the surfaces defined by the speakerss.

     Normal - The virtual point can move through the full 2D/3D space. This is done by
     simulating amp decrease and delay from sound travelling through air.

     DistComp - VBAP assumes that the speakers are equidistant to the center. If this is
     not the case then then they should be delayed by (D - d)/c where D is the distance
     of the speaker farthest away, d is the distance of each speaker and c is the speed
     of sound in the air.
     */
	*generateDefs { |numSpeakers|

	    var pointUgenFunc = {
	        var point, pointFromBus, pointLag;
	        pointFromBus = \pointFromBus.kr( 0 );
            point = (\point.kr( [0,0] ) * (1-pointFromBus))
                + ( UIn.kr(0,2) * pointFromBus );
            pointLag = \pointLag.kr( pointLag );
            LPFLag.kr( point, pointLag ).asPoint;
	    };

        var point3DUgenFunc = {
	        var point, pointFromBus, pointLag;
	        pointFromBus = \pointFromBus.kr( 0 );
            point = (\point.kr( [0,0,0] ) * (1-pointFromBus))
                + ( UIn.kr(0,3) * pointFromBus );
            pointLag = \pointLag.kr( pointLag );
            LPFLag.kr( point, pointLag ).as(RealVector3D);
	    };

	    var anglesUgenFunc = {
	        var angles, anglesFromBus, anglesLag;
	        anglesFromBus = \anglesFromBus.kr( 0 );
            angles = (\angles.kr( [0,0] ) * (1-anglesFromBus))
                + ( UIn.kr(0,2) * anglesFromBus );
            anglesLag = \lag.kr( 0 );
            LPFLag.kr( angles, anglesLag );
	    };

	    var simpleOut = { |out, azi, elev|
	        out = UGlobalEQ.ar( out );
            out = VBAP.ar(numSpeakers, out, \u_bufnum.kr(0), azi, elev, \spread.kr(0) );
            out = out * UEnv.kr( extraSilence: 0.2 );
            Out.ar( \bus.kr(0), out );
	    };

	    var distCompOut = { |out, azi, elev, delays|
	        out = UGlobalEQ.ar( out );
            out = VBAPDistComp2.ar(numSpeakers, out, \u_bufnum.kr(0), azi, elev, \spread.kr(0), delays );
            out = out * UEnv.kr( extraSilence: 0.2 );
            Out.ar( \bus.kr(0), out );
	    };

	    var prePanner = { |out, point|
	        // the pre-panner and delayed/attenuated output
            WFSPrePan( \dbRollOff.kr( -6 ), \maxAmpRadius.kr( 2 ), \latencyComp.ir( 0 ) ).ar( out, point );
	    };

		^[SynthDef(("VBAP_SIMPLE_3D_"++numSpeakers).asSymbol, {
            var angles = anglesUgenFunc.();
            simpleOut.( UIn.ar( 0 ), angles[0], angles[1] );
        } ),

        SynthDef(("VBAP_SIMPLE_2D_"++numSpeakers).asSymbol, {
            simpleOut.( UIn.ar( 0 ), pointUgenFunc.().theta/2pi*360, 0 );
        } ),

        SynthDef(("VBAP_SIMPLE_3D_DistComp_"++numSpeakers).asSymbol, {
            var angles = anglesUgenFunc.();
            var speakerDelays = \u_delays.ir( numSpeakers.collect{0.0} );
            distCompOut.( UIn.ar( 0 ), angles[0], angles[1], speakerDelays  );
        } ),

        SynthDef(("VBAP_SIMPLE_2D_DistComp_"++numSpeakers).asSymbol, {
            var point = pointUgenFunc.();
            var speakerDelays = \u_delays.ir( numSpeakers.collect{0.0} );
            simpleOut.( UIn.ar( 0 ), point, speakerDelays );
        } ),

        SynthDef(("VBAP_2D_"++numSpeakers).asSymbol, {
            var point = pointUgenFunc.value;
            var out = UIn.ar( 0 );
            out = prePanner.( out, point );
            simpleOut.( out, point.theta/2pi*360, 0 );
        } ),

        SynthDef(("VBAP_2D_DistComp_"++numSpeakers).asSymbol, {
            var point = pointUgenFunc.value;
            var speakerDelays = \u_delays.ir( numSpeakers.collect{0.0} );
            var out = UIn.ar( 0 );
            out = prePanner.( out, point );
            out = distCompOut.(out, point.theta/2pi*360, 0, speakerDelays);
        } ),

        SynthDef(("VBAP_3D_"++numSpeakers).asSymbol, {
            var point = point3DUgenFunc.value;
            var out = UIn.ar( 0 );
            out = prePanner.( out, point );
            [point.theta/2pi*360.poll, point.phi/2pi*360];
            simpleOut.( out, point.theta/2pi*360.poll, point.phi/2pi*360 );
        } ),

        SynthDef(("VBAP_3D_DistComp_"++numSpeakers).asSymbol, {
            var point = point3DUgenFunc.value;
            var speakerDelays = \u_delays.ir( numSpeakers.collect{0.0} );
            var out = UIn.ar( 0 );
            out = prePanner.( out, point );
            out = distCompOut.(out, point.theta/2pi*360, point.phi/2pi*360, speakerDelays);
                } )
        ]
	}

	*generateAll { |maxNumSpeakers = 55, dir|
        dir = dir ? SynthDef.synthDefDir;
        (4..maxNumSpeakers).collect { |i|
            this.generateDefs( i ).do{ |def| def.writeDefFile( dir ) };
        }
    }

}