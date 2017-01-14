#
# lockstep_test.py
#
# Author: xuwei06 (wei.xu@baidu.com)
# Created on: 2017-01-12
#
# Copyright (c) Baidu.com, Inc. All Rights Reserved

import MalmoPython

import math
import os
import sys
import time

missionXML = '''<?xml version="1.0" encoding="UTF-8" standalone="no" ?>
<Mission xmlns="http://ProjectMalmo.microsoft.com" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">

  <About>
    <Summary>Cliff walking mission based on Sutton and Barto.</Summary>
  </About>
        <ModSettings>
            <MsPerTick>1</MsPerTick>
        </ModSettings>

  <ServerSection>
    <ServerInitialConditions>
      <Time>
        <StartTime>1</StartTime>
        <AllowPassageOfTime>false</AllowPassageOfTime>
      </Time>
    </ServerInitialConditions>
    <ServerHandlers>
      <FlatWorldGenerator generatorString="3;7,220*1,5*3,2;2;,biome_1"/>
      <!-- The actual timeLimit is: timeLimitMs * MsPerTick / 50-->
      <ServerQuitFromTimeUp timeLimitMs="2000000"/>
      <ServerQuitWhenAnyAgentFinishes/>
    </ServerHandlers>
  </ServerSection>

  <AgentSection mode="Survival">
    <Name>Cristina</Name>
    <AgentStart>
      <Placement x="0.5" y="227" z="0.5" pitch="30" yaw="0"/>
    </AgentStart>
    <AgentHandlers>
      <ContinuousMovementCommands/>
      <MissionQuitCommands/>
      <VideoProducer  want_depth="0" viewpoint="0">
        <Width> 128 </Width>
        <Height> 128 </Height>
      </VideoProducer>
    <ObservationFromFullStats/>
      <RewardForTouchingBlockType>
        <Block reward="-4" type="cobblestone" behaviour="onceOnly"/>
        <Block reward="4" type="lapis_block" behaviour="onceOnly"/>
      </RewardForTouchingBlockType>
      <AgentQuitFromTouchingBlockType>
          <Block type="cobblestone" />
          <Block type="lapis_block" />
      </AgentQuitFromTouchingBlockType>
      <RewardForSendingCommand reward="-0.1" />
    </AgentHandlers>
  </AgentSection>

</Mission>
'''

agent_host = MalmoPython.AgentHost()
try:
    agent_host.parse( sys.argv )
except RuntimeError as e:
    print 'ERROR:',e
    print agent_host.getUsage()
    exit(1)
if agent_host.receivedArgument("help"):
    print agent_host.getUsage()
    exit(0)

my_mission = MalmoPython.MissionSpec(missionXML, True)
my_mission_record = MalmoPython.MissionRecordSpec()

# Attempt to start a mission:
max_retries = 3
for retry in range(max_retries):
    try:
        agent_host.startMission( my_mission, my_mission_record )
        break
    except RuntimeError as e:
        if retry == max_retries - 1:
            print "Error starting mission:",e
            exit(1)
        else:
            time.sleep(2)

# Loop until mission starts:
print "Waiting for the mission to start ",
world_state = agent_host.getWorldState()
while not world_state.has_mission_begun:
    sys.stdout.write(".")
    time.sleep(0.1)
    world_state = agent_host.getWorldState()
    for error in world_state.errors:
        print "Error:",error.text

print
print "Mission running "

for speed in [0.1, 0.2, 0.3, 1.0]:
    agent_host.sendCommand("move %s" % speed)
    for i in xrange(7):
        agent_host.sendCommand("advance")
    time.sleep(1)
    s = agent_host.getWorldState()
    x0 = s.video_frames[-1].xPos
    z0 = s.video_frames[-1].zPos
    t0 = time.time()
    wait = 0
    for i in xrange(100):
        agent_host.sendCommand("advance")
        while True:
            s = agent_host.getWorldState()
            if len(s.video_frames) > 0:
                x = s.video_frames[-1].xPos
                z = s.video_frames[-1].zPos
                break;
            else:
                time.sleep(0.001)
                wait += 1
    t1 = time.time()

    print "speed=%s" % speed,\
          "distance=%s" % math.sqrt((x-x0)*(x-x0)+(z-z0)*(z-z0)),\
          "t=%s" % (t1 - t0),\
          "wait=%s" % (0.001 * wait)
