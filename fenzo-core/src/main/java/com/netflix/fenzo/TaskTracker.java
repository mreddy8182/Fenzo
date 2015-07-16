/*
 * Copyright 2015 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.fenzo;

import com.netflix.fenzo.sla.ResAllocs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @warn class description missing
 */
public class TaskTracker {

    /**
     * @warn class description missing
     */
    public static class TaskGroupUsage implements ResAllocs {
        private final String taskGroupName;
        private double cores=0.0;
        private double memory=0.0;
        private double networkMbps=0.0;
        private double disk=0.0;

        private TaskGroupUsage(String taskGroupName) {
            this.taskGroupName = taskGroupName;
        }

        /**
         * @warn method description missing
         *
         * @return
         */
        @Override
        public String getTaskGroupName() {
            return taskGroupName;
        }

        /**
         * @warn method description missing
         *
         * @return
         */
        @Override
        public double getCores() {
            return cores;
        }

        /**
         * @warn method description missing
         *
         * @return
         */
        @Override
        public double getMemory() {
            return memory;
        }

        /**
         * @warn method description missing
         *
         * @return
         */
        @Override
        public double getNetworkMbps() {
            return networkMbps;
        }

        /**
         * @warn method description missing
         *
         * @return
         */
        @Override
        public double getDisk() {
            return disk;
        }

        /**
         * @warn method description missing
         * @warn parameter description missing
         *
         * @param task
         */
        void addUsage(TaskRequest task) {
            cores += task.getCPUs();
            memory += task.getMemory();
            networkMbps += task.getNetworkMbps();
            disk += task.getDisk();
        }

        /**
         * @warn method description missing
         * @warn parameter description missing
         *
         * @param task
         */
        void subtractUsage(TaskRequest task) {
            cores -= task.getCPUs();
            if(cores < 0.0) {
                logger.warn("correcting cores usage <0.0");
                cores=0.0;
            }
            memory -= task.getMemory();
            if(memory<0.0) {
                logger.warn("correcting memory usage<0.0");
                memory=0.0;
            }
            networkMbps -= task.getNetworkMbps();
            if(networkMbps<0.0) {
                logger.warn("correcting networkMbps usage<0.0");
                networkMbps=0.0;
            }
            disk -= task.getDisk();
            if(disk<0.0) {
                logger.warn("correcting disk usage<0.0");
                disk=0.0;
            }
        }
    }

    /**
     * @warn class description missing
     */
    public static class ActiveTask {
        private TaskRequest taskRequest;
        private AssignableVirtualMachine avm;
        public ActiveTask(TaskRequest taskRequest, AssignableVirtualMachine avm) {
            this.taskRequest = taskRequest;
            this.avm = avm;
        }

        /**
         * @warn method description missing
         *
         * @return
         */
        public TaskRequest getTaskRequest() {
            return taskRequest;
        }

        /**
         * @warn method description missing
         *
         * @return
         */
        public VirtualMachineLease getTotalLease() {
            return avm.getCurrTotalLease();
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(TaskTracker.class);
    private final Map<String, ActiveTask> runningTasks = new HashMap<>();
    private final Map<String, ActiveTask> assignedTasks = new HashMap<>();
    private final Map<String, TaskGroupUsage> taskGroupUsages = new HashMap<>();

    // package scoped
    TaskTracker() {
    }

    /**
     * @warn method description missing
     * @warn parameter descriptions missing
     *
     * @param request
     * @param avm
     * @return
     */
    boolean addRunningTask(TaskRequest request, AssignableVirtualMachine avm) {
        final boolean added = runningTasks.put(request.getId(), new ActiveTask(request, avm)) == null;
        if(added)
            addUsage(request);
        return added;
    }

    /**
     * @warn method description missing
     * @warn parameter description missing
     *
     * @param taskId
     * @return
     */
    boolean removeRunningTask(String taskId) {
        final ActiveTask removed = runningTasks.remove(taskId);
        if(removed != null) {
            final TaskRequest task = removed.getTaskRequest();
            final TaskGroupUsage usage = taskGroupUsages.get(task.taskGroupName());
            if(usage==null)
                logger.warn("Unexpected to not find usage for task group " + task.taskGroupName() +
                        " to remove usage of task " + task.getId());
            else
                usage.subtractUsage(task);
        }
        return removed != null;
    }

    /**
     * @warn method description missing
     *
     * @return
     */
    Map<String, ActiveTask> getAllRunningTasks() {
        return Collections.unmodifiableMap(runningTasks);
    }

    /**
     * @warn method description missing
     * @warn parameter descriptions missing
     *
     * @param request
     * @param avm
     * @return
     */
    boolean addAssignedTask(TaskRequest request, AssignableVirtualMachine avm) {
        final boolean assigned = assignedTasks.put(request.getId(), new ActiveTask(request, avm)) == null;
        if(assigned)
            addUsage(request);
        return assigned;
    }

    private void addUsage(TaskRequest request) {
        TaskGroupUsage usage = taskGroupUsages.get(request.taskGroupName());
        if(usage==null) {
            taskGroupUsages.put(request.taskGroupName(), new TaskGroupUsage(request.taskGroupName()));
            usage = taskGroupUsages.get(request.taskGroupName());
        }
        usage.addUsage(request);
    }

    /**
     * @warn method description missing
     */
    void clearAssignedTasks() {
        for(ActiveTask t: assignedTasks.values())
            taskGroupUsages.get(t.getTaskRequest().taskGroupName()).subtractUsage(t.getTaskRequest());
        assignedTasks.clear();
    }

    /**
     * @warn method description missing
     *
     * @return
     */
    Map<String, ActiveTask> getAllAssignedTasks() {
        return Collections.unmodifiableMap(assignedTasks);
    }

    /**
     * @warn method description missing
     * @warn parameter description missing
     *
     * @param taskGroupName
     * @return
     */
    public TaskGroupUsage getUsage(String taskGroupName) {
        return taskGroupUsages.get(taskGroupName);
    }
}
