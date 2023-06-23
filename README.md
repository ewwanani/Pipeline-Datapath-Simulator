# Pipeline-Datapath-Simulator
Simulate how a pipelined datapath works in Java
This is a project for CS 472 Computer Architecture, BU.
The project has a function for each step in the pipeline: IF(Instruction Fetch), ID(Instruction Decode), EX(Execute), MEM(Memory), and WB(WriteBack). The main program has some initialization code and then is one big loop, where each time through the loop is equivalent to one cycle in a pipeline. That loop can call those five functions, print out the appropriate information (the 32 registers and both READ and WRITE versions of the four pipeline registers) and then copy the WRITE version of the pipeline registers into the READ version for use in the next cycle.
