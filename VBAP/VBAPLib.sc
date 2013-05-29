/*
VBAPSynthDef.writeDefs(32);
VBAPSynthDef.writeDefs(5);
VBAPSynthDef.writeDefs(8);
VBAPSynthDef.writeDefs(4);
)
(
Routine({
//Server.scsynth;
VBAPLib.startupR(\fivePointOne)
}).play(AppClock)
)

(
Routine({
Server.scsynth;
VBAPLib.startupR(\octo)
}).play(AppClock)
)

(
Routine({
Server.scsynth;
VBAPLib.startupR(\soniclabSlave)
}).play(AppClock)
)

(
UScore(*[
	UChain([ 'bufSoundFile', [ 'soundFile', BufSndFile.newBasic("/usr/local/share/SuperCollider/sounds/a11wlk01-44_1.aiff", 107520, 1, 44100, 0, nil, 1, true) ] ], [ 'vbap2D_Simple_Panner', [ 'point', Point(-2.1, 16.6), 'lag', 2.0 ] ])]).gui
)
*/
VBAPLib {

	*startupR { |options|

		var defs;

		if( options.isKindOf(Symbol) ) {
			options = VBAPOptions.fromPreset(options)
		};

		UChain.makeDefaultFunc = {
			UChain( \bufSoundFile, \stereoOutput ).useSndFileDur
		};

		this.prStartupServers(options);

		if(options.isSlave.not) {
			this.prStartupGUIs;
			CmdPeriod.add(this);
		}
	}

	*startup { |options|
		Routine({
			VBAPLib.startup(options)
		}).play(AppClock)
	}

	*prStartupServers{ |options| //servers, send = true, allDefs = true|

		var serverOptions = this.serverOptions( options.numOutputChannels, options.device );

		var servers = options.serverDescs.collect{ |desc|
			Server(desc[0], NetAddr(desc[1], desc[2] ), serverOptions )
		};

		servers.do{ |s|
			if(s.isLocal){
				2.0.wait;
				s.boot;
			}
		};

		ULib.servers = [ LoadBalancer(*servers) ];
		Server.default = servers[0];
		thisProcess.interpreter.s = servers[0];

        "\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n".postln;
		ULib.serversWindow;
		//client and single startup
		if(options.isSlave.not) {

			VBAPSpeakerConf.default = VBAPSpeakerConf(options.angles, options.distances);



			"*** Will start waiting for servers".postln;
			ULib.waitForServersToBoot;
			"*** Servers booted\n".postln;

			//Udef SYNTHEDEFS
			"*** Sending synthDefs".postln;
			this.loadDefs(options);
			"*** SynthDefs Sent\n".postln;


			//VBAP BUFFERS
			"*** Creating vbap buffers".postln;
			VBAPSpeakerConf.default.sendBuffer(servers);
			"*** VBAP buffers created\n".postln;
		}
	}

	*prStartupGUIs {
        if( (thisProcess.platform.class.asSymbol == 'OSXPlatform') && {
				thisProcess.platform.ideName.asSymbol === \scapp
		}) {
			UMenuBar();
		} {
			UMenuWindow();
		};

        UGlobalGain.gui;
        UGlobalEQ.gui;
        //ULib.serversWindow;
    }

	*serverOptions { |numOutputChannels, device|
		^ServerOptions()
		.memSize_(8192*16)
		.numWireBufs_(64*2)
		.numPrivateAudioBusChannels_(1024)
		.outDevice_(device)
		.inDevice_(device)
		.numOutputBusChannels_(numOutputChannels)
    }

	//only needs to be run once.
	*writeDefs { |n = 32|
        Udef
		.loadAllFromDefaultDirectory
		.collect(_.synthDef)
		.flat.select(_.notNil)
		.do({|def| def.writeDefFile; });
        VBAPSynthDef.writeDefs(n);
	}

	*cmdPeriod { Server.freeAllRemote( false ); }

    *loadDefs { |options|
        var defs;

		Udef.defsFolders = if(options.loadDefsAtStartup){ Udef.defsFolders }{[]} ++ [
            WFSArrayPan.filenameSymbol.asString.dirname +/+ "UnitDefs"
		] ++ options.extraDefFolders;

		Udef.userDefsFolder = File.getcwd +/+ "UnitDefs";

		Udef.defsFolders.add(
            VBAPLib.filenameSymbol.asString.dirname +/+ "UnitDefs"
        );

       Udef.loadAllFromDefaultDirectory

    }

}