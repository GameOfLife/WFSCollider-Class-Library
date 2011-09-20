UndoManager {
	
	var <history, <current = 0;
	var <>max = 50;
	var <>verbose = false;
	
	add { |obj, msg|
		history = [ [ obj.deepCopy, msg, Date.localtime ] ] ++ (history ? [])[current..];
		current = 0;
		if( history.size > max ) { history = history[..max+1] };
		this.changed( \add );
	}
		
	undo { |num = 1|
		var obj;
		if( (num + current).exclusivelyBetween( -1, history.size ) ) { 
			current = num + current;
			obj = history[current][0].copy;
			this.changed( \undo );
			^obj;
		} { 
			if( this.verbose ) {
				"% :: minimum or maximum reached\n".postf( thisMethod.asString );
			};
			^nil;
		};
	}
	
	previous { ^this.undo(1); }
	next { ^this.undo(-1); }
	
	at { |index = 0|
		^history[ index ] !? { history[ index ][ 0 ] };
	}
	
	clear {
		history = [];
		current = 0;
		this.changed( \clear );
	}
	
	post {
		"UndoManager history:".postln;
		 history.reverseDo({ |item, i|
			var size;
			size = history.size;
			"\t%: % (%)\n".postf( size - i, (item[1] ? "").asString, item[2].hourStamp[..7] );
		});
	}
	
}