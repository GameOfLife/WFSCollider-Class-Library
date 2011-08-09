// MultiActionFunc remembers how many times an action is added,
// and fires only if the action was called as many times
// example:

/*

(
// "done" is posted only once after all buffers are loaded
a = MultiActionFunc( { |...args| args.postln; nil; } );

5.do({
	Buffer.alloc( s, 44100, 1 ). a.getAction );
});

)



*/


MultiActionFunc {
	var <>action, <>routine, <>i = 0, <>n = 0;
	
	*new { |action| 
		^super.newCopyArgs( action ); 
	}
	
	getAction {
		n = n + 1;
		^{ |...args|
			if( i < (n-1) ) { 
				i = i+1; nil;
			} {
				action.value( *args );
			};
		};
	}
	
}
