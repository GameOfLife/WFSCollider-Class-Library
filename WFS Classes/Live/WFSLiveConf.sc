WFSLiveConf {

	*getCurrent{

		^if( WFSServers.default.isSingle ) {
			WFSLiveConfOffline()
		} {
			WFSLiveConfOnline()
		}
	}

	routerOffset{ }

	inOffset{ }

	getServers{ |i| }

	sync{ }

	getWFSConf{ }

}

//for offline use, i.e. just one computer and a stereo or other.
WFSLiveConfOffline : WFSLiveConf{
	var <masterServer;

	*new{
		^super.new.init
	}

	init{
		masterServer = WFSServers.default.masterServer;
	}

	routerOffset{
		var options = masterServer.options;
		^options.numInputBusChannels + options.numOutputBusChannels
	} //send input to first private bus

	inOffset{ ^NumOutputBuses.ir + NumOutputBuses.ir } //  and then copy from there

	getServers{ ^[masterServer] }

	sync{ ^masterServer.sync }

	getWFSConf{ ^WFSServers.default.singleWFSConfiguration }

}

//for macbook +  2 mac pros with the 192 speakers system
WFSLiveConfOnline : WFSLiveConf{
	var servers;

	*new{
		^super.new.init
	}

	init{
		servers = WFSServers.default.multiServers.collect(_.servers).flop;
	}

	routerOffset{ ^14 } //send via first adat out of macbook which is output 14.

	inOffset{ ^NumOutputBuses.ir } //receive in the first input of mac.

	getServers{ |i| ^servers[i] }

	sync{ servers.flat.do( _.sync ) }

	getWFSConf{ ^WFSServers.default.wfsConfigurations[0] }

}

WFSLiveConfSingleMachine : WFSLiveConf{
	var servers;

	*new{
		^super.new.init
	}

	init{
		servers = WFSServers.default.multiServers.collect(_.servers).flop;
	}

	routerOffset{ ^0 } //send via first adat out of macbook which is output 14.

	inOffset{ ^NumOutputBuses.ir } //receive in the first input of mac.

	getServers{ |i| ^servers[i.div(3)] }

	sync{ servers.flat.do( _.sync ) }

	getWFSConf{ ^WFSServers.default.wfsConfigurations[0] }

}