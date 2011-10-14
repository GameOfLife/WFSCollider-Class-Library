WFSMultiPath {
	var <>paths;
	var <>fillTime = 0.1;
	
	prPathIndex { |index = 0|
		var sizes, pathIndex;
		sizes = paths.collect({ |item| item.positions.size }).integrate;
		pathIndex = sizes.detectIndex( index < _ );
		^[ pathIndex, index - (sizes[ pathIndex - 1 ] ? 0) ]
	}
	
	at { |index| 
		^paths.at(index) 
	}
	
	positions {
		^paths.collect(_.positions).flatten(1);
	}
	
	positions_ { |positions|
		positions.clumps( paths.collect(_.positions.size) ).do({ |item, i|
			paths[i].positions = item;
		});	
	}
	
	times {
		var times = [];
		paths.do({ |pth|
			times ++ pth.times ++ [fillTime]
		});
		^times;
	}

	forceTimes {
		// todo
	}
	
	length {
		^paths.collect(_.length).maxItem;
	}
	
}