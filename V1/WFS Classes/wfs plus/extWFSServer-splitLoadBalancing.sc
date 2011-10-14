/*
    GameOfLife WFSCollider - Wave Field Synthesis spatialization for SuperCollider.
    The Game Of Life Foundation. http://gameoflife.nl
    Copyright 2006-2010 Wouter Snoei.

    GameOfLife WFSCollider software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    GameOfLife WFSCollider is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with GameOfLife WFSCollider.  If not, see <http://www.gnu.org/licenses/>.
*/

+ WFSServers {
	
	addDictActivity { |value = 0, server| 
		activityDict[ server ] !? { activityDict[ server ] = activityDict[ server ] + value } }
	
	
	removeDictActivity { |value = 0, servers|
			value = value.asCollection;
			servers = servers.asCollection;
			servers.do({ |srv, i|
				this.prRemoveDictActivity( value.wrapAt(i), srv );
				});
			// activityDict.postln;
			}
			
	prRemoveDictActivity { |value = 0, server| 
		activityDict[ server ] !? { activityDict[ server ] = activityDict[ server ] - value } }
		
	setDictActivity { |value = 0, server| activityIndex[ server ] = value; }
	
	resetDictActivity {
			activityDict = IdentityDictionary[];
			multiServers.do({ |ms| ms.servers.do({ |srv| activityDict[ srv ] = 0 }); });
			}
	
	
	leastActiveServer { |index = 0|
			var dict, lowestVal = inf, srv;
			
			dict = activityDict.select({ |value, key|
						multiServers[index].servers.includes( key );
						});
			
			dict.sortedKeysValuesDo({ |key, value|
					if( value < lowestVal )
						{ srv = key; lowestVal = value; };
					}, { |a,b| a.name <= b.name });
			^srv;
			}
	
	leastActiveServers {
		^multiServers.size.collect({ |i| this.leastActiveServer( i ) });
		}
	
	nextDictServer { |addActivity = 0, index = 0|
		var srv;
		srv = this.leastActiveServer( index );
		this.addDictActivity( addActivity, srv );
		^srv;
		}
	
	nextDictServers { |addActivity = 0|
		var srv;
		srv = this.leastActiveServers;
		srv.do({ |srv, i| this.addDictActivity( addActivity.asCollection.wrapAt(i), srv ); });
		^srv;
		}
		
	syncDelayOf { |srv|
		var ii;
		multiServers.do({ |ms, i|
			var index;
			if( (index = ms.servers.detectIndex({ |item| item == srv })).notNil )
				{ ii = [i,index] };
			});
			
		if( ii.notNil )
			{ ^syncDelays[ ii[0] ][ ii[1] ]; }
			{ "WARNING: syncdelay for server % not found\n".postf( srv ); ^0 };
		}

	
	}