WFSLive {

	classvar wfsLiveOsc, <isStarted = false, <trackNum = 12, <wfsLiveConf;
	classvar <tracks;

	*startup {

		var router, routerOffset;

		wfsLiveConf = WFSLiveConfSingleMachine.new;

		WFS.mode = \live;

		// initialize
		tracks = trackNum.collect{ |i| WFSLiveTrack( i, wfsLiveConf.getServers(i), /* servers[i] */
			pos: (cos(2pi*i/trackNum)@sin(2pi*i/trackNum))*7 )
		};

		WFSLiveOSCResp.init;
		isStarted = true;
	}

	*free{
		tracks.do( _.endAndFreeBuffers );

		WFSLiveOSCResp.free;
		WFS.mode = \score;
		isStarted = false;
	}

	*start {

		fork{
			tracks.do( _.loadBuffers );
			wfsLiveConf.sync;
			tracks.do( _.start )
		}
	}

	*stop {
		tracks.do( _.endAndFreeBuffers );
	}

	*positions{
		^tracks.collect( _.pos )
	}

	*synthDefs {

		var wfsLiveConf = WFSLiveConf.getCurrent;
		var wfsConfig = wfsLiveConf.getWFSConf;

		^[
		SynthDef( "wfsLive_point", { |i_bufD = 0, i_bufNumSw = 0, gate = 1, x = 0, y = 0, i_in = 0, level = 1, lag = 0.5|
			var sig, busLevel, scaledIn, env, pos, switch, normal;

			sig = In.ar( wfsLiveConf.inOffset + i_in );
			env = EnvGen.kr( Env.asr(0.01,1,0.01), gate, doneAction: 2 );
			busLevel  = WFSLevelBus.kr;
			scaledIn = WFSEQ.unit( sig * level.lag(lag) * env * busLevel );
			pos = WFSPoint( x.lag(lag), y.lag(lag) );
			normal = WFSPan2D.arBufC( scaledIn, i_bufD, pos, wfsConfig, 0);
			switch = WFSPan2D.arBufSwitch(scaledIn, i_bufNumSw, pos, wfsConfig, 0);
			Out.ar( 0, normal + switch );

		}),
		SynthDef( "wfsLive_plane", { |i_bufD = 0, gate = 1, x = 0, y = 0, i_in = 0, level = 1, lag = 0.5|
			var sig, busLevel, scaledIn, env, panned, pos, point;

			sig = In.ar( wfsLiveConf.inOffset + i_in );
			env = EnvGen.kr( Env.asr(0.01,1,0.01), gate, doneAction: 2 );
			busLevel  = WFSLevelBus.kr;
			scaledIn = WFSEQ.unit( sig * level.lag(lag) * env * busLevel );
			point = WFSPoint(x.lag(lag), y.lag(lag) );
			pos = WFSPlane.newCopyArgs( atan2(point.x ,point.y.neg ) , point.distance );
			panned = WFSPan2D.arBufC( scaledIn, i_bufD, pos, wfsConfig, 0);

			Out.ar( 0, panned );

		})
		]
	}

	*writeSynthDefs {

		this.synthDefs.do( _.writeDefFile )

	}

	*loadSynthDefs { |server|

		this.synthDefs.do( _.load( server ) )

	}
}