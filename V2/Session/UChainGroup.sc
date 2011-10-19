UChainGroup : UEvent {
    var <chains;

    *new { |...chains|
        ^super.new.init(chains)
    }

    init { |inChains|
        chains = inChains;
    }

    name { ^chains.collect(_.name).asString }

    waitTime { ^chains.collect(_.waitTime).sum }

    prepare { |target, startPos = 0, action|
        chains.do(_.prepare(target, startPos, action))
    }

    prepareAndStart{ |target, startPos = 0|
        chains.do(_.prepareAndStart(target, startPos))
    }

    prepareWaitAndStart { |target, startPos = 0|
        chains.do(_.prepareWaitAndStart(target, startPos))
    }

    start { |target, startPos, latency|
        chains.do(_.start(target, startPos, latency))
    }

    release { |time|
        chains.do(_.release(time))
    }

    dispose { chains.do(_.dispose) }

    dur { ^chains.collect(_.duration).maxItem }

    dur_ { |dur| chains.do(_.duration_(dur)) }

    groups { ^chains.collect(_.groups).flat }

    gui { ^MassEditUChain(chains).gui } 
}