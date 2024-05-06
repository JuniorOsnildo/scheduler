package org.scheduler;
import java.util.Arrays;
import java.util.LinkedList;


public class Scheduler {
    private final int simulation_time;
    private final String scheduler_name;
    private final int tasks_number;
    private int time;

    private float cpu_usage;
    private int quantum_timer;
    private Task[] cpu;

    public Task[] tasks;

    private Log logs;
    private LinkedList<Task> waitingQueue;
    private LinkedList<Task> finishedQueue;

    private void starting() {
        time = 0;
        cpu_usage = simulation_time;
        quantum_timer = 0;
        cpu = new Task[1];
        logs = new Log(tasks_number);
        waitingQueue = new LinkedList<>();
        finishedQueue = new LinkedList<>();
    }

    public Scheduler(int simulation_time, String scheduler_name, int tasks_number, Task[] tasks) {
        this.simulation_time = simulation_time;
        this.scheduler_name = scheduler_name;
        this.tasks_number = tasks_number;
        this.tasks = tasks;
    }

    @Override
    public String toString() {
        return "Escalonador{" +
                "simulation_time=" + simulation_time +
                ", scheduler_name='" + scheduler_name + '\'' +
                ", task_number=" + tasks_number +
                ", Tasks=" + Arrays.toString(tasks) +
                '}';
    }

    private void incrementWT() {
        for (Task t:waitingQueue) {
            for (Task u : tasks) {
                if(t.getIndex() == u.getIndex())
                    u.setWaiting_time(u.getWaiting_time() + 1);
            }
        }

    }

    private void incrementTAT(Task task) {
        for (Task u : tasks) {
            if(task.getIndex() == u.getIndex())
                u.setTurnaround_time(u.getTurnaround_time() + 1);
        }
    }


    private void deadlineCheck(){
        for (Task t:waitingQueue) {
            if(time == t.getAbsolute_deadline()+1){

                for (Task u:tasks) {
                    if(t.getIndex() == u.getIndex())
                        u.setDeadline_loss(u.getDeadline_loss() + 1);
                }
                t.setDeadline_loss(t.getDeadline_loss() + 1);
                logs.deadline_loss_list.add("Task: "+t.getIndex()+" lost deadline at time: "+time);
            }
        }
        if(cpu[0] != null)
            if(time == cpu[0].getAbsolute_deadline()+1){
                for (Task u: tasks) {
                    if(cpu[0].getIndex() == u.getIndex())
                        u.setDeadline_loss(u.getDeadline_loss() + 1);
                }

                logs.deadline_loss_list.add("Task: "+cpu[0].getIndex()+" lost deadline at time: "+time);
            }
    }

    private boolean isSchedulableRM(){
        float n = tasks.length;

        double left = 0;
        double right = n*(Math.pow(2,(1/n))-1);

        for (Task t:tasks){
            left += ((double) t.getComputation_time() /t.getPeriod_time());
        }

        return left <= right;
    }

    private boolean isSchedulableEDF(){
        double left = 0;


        for (Task t:tasks){
            left += ((double) t.getComputation_time() /t.getPeriod_time());
        }

        return left <= 1;
    }


    public void schedule() throws CloneNotSupportedException {
        starting();

        boolean schedulable = true;


        if(this.scheduler_name.equals("rm")) { schedulable = isSchedulableRM(); }
        if(this.scheduler_name.equals("edf")) { schedulable = isSchedulableEDF(); }


        while (time <= simulation_time && schedulable) {

            if (time == 0) {
                for (Task t : tasks) {
                    if (t.getOffset() == time) {

                        System.out.println("Task "+t.getIndex()+" add to Waiting list at time: "+time);

                        t.generateDeadline(time);

                        waitingQueue.add((Task) t.clone());

                        t.incrementOffSet();
                        t.setExecutions(t.getExecutions() + 1);

                    }
                }

            } else {
                for (Task t : tasks) {
                    if (time % t.getOffset() == 0) {

                        System.out.println("Task "+t.getIndex()+" add to Waiting list at time: "+time);

                        t.generateDeadline(time);

                        waitingQueue.add((Task) t.clone());
                        t.incrementOffSet();
                        t.setExecutions(t.getExecutions() + 1);

                    }
                }
            }


            switch (scheduler_name) {
                case "fcfs":
                    break;
                case "rr":
                    schedulerRR();
                    break;
                case "rm":
                    schedulerRM();
                    break;
                case "edf":
                    schedulerEDF();
                    break;
            }


            if (cpu[0] == null) {
                cpu[0] = waitingQueue.poll();
            }

            if (cpu[0] != null) {

                cpu[0].setProgress(cpu[0].getProgress() + 1);
                quantum_timer++;

                incrementTAT(cpu[0]);

                if (cpu[0].getProgress() == cpu[0].getComputation_time()) {

                    System.out.println("Task "+cpu[0].getIndex()+" Finished at time: "+time);



                    finishedQueue.add(cpu[0]);
                    cpu[0] = null;
                    quantum_timer = 0;
                    }

            } else {
                cpu_usage--;
            }

            incrementWT();

            for (Task t: waitingQueue) {
                incrementTAT(t);
            }


            deadlineCheck();

            time++;

        }

        if(schedulable) {
            logs.showLogs(tasks);
            logs.thereIsStarvation(waitingQueue);
            logs.cduUsage(cpu_usage, simulation_time);

            System.out.println("\nFinished task list: \n" + finishedQueue);
        } else {
            System.out.println("This tasks aren't schedulable!");
        }

    }

    private void schedulerRR() {
        if (cpu[0] != null){
            if (cpu[0].getQuantum() == quantum_timer) {
                waitingQueue.add(cpu[0]);
                cpu[0] = waitingQueue.poll();
                quantum_timer = 0;
            }
        }
    }


    private void schedulerRM(){

        if(cpu[0] == null) { cpu[0] = waitingQueue.poll(); }

        LinkedList<Task> aux = new LinkedList<>();

        for (Task t:waitingQueue) {
            if (t.getPeriod_time() < cpu[0].getPeriod_time()){
                aux.add(cpu[0]);
                cpu[0] = t;
                waitingQueue.remove(t);
            }
        }
        if(cpu[0] != null)
            System.out.println("Task "+cpu[0].getIndex()+" add to Waiting list at time: "+time);

        waitingQueue.addAll(aux);

    }

    private void schedulerEDF(){
        if(cpu[0] == null) { cpu[0] = waitingQueue.poll(); }

        LinkedList<Task> aux = new LinkedList<>();

        for (Task t:waitingQueue) {
            if ( (t.getAbsolute_deadline()-time) < (cpu[0].getAbsolute_deadline()-time) ){
                aux.add(cpu[0]);
                cpu[0] = t;
                waitingQueue.remove(t);
            }
        }
        if(cpu[0] != null)
            System.out.println("Task "+cpu[0].getIndex()+" add to Waiting list at time: "+time);

        waitingQueue.addAll(aux);
    }
}


