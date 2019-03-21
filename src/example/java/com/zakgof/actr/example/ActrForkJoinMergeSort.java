package com.zakgof.actr.example;

import java.util.Arrays;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import com.zakgof.actr.ActorRef;
import com.zakgof.actr.ActorSystem;
import com.zakgof.actr.Actr;

public class ActrForkJoinMergeSort {

	public static void main(String[] args) throws InterruptedException {
		Random random = new Random(0L);
		int[] input = IntStream.range(0, 1 << 10).map(i -> random.nextInt(1 << 20)).toArray();
		System.err.println("Actr merge sort started...");
		long start = System.currentTimeMillis();
		sort(input);
		long end = System.currentTimeMillis();
		System.err.println("finished in " + (end - start));
	}
	
	public static void sort(int[] input) {

		final ActorSystem system = ActorSystem.create("actrsort");
		
		final ActorRef<MasterActor> master = system.actorOf(MasterActor::new, "master");
		master.tell(m -> m.start(input));
		system.shutdownCompletable().join();
	}

	private static class MasterActor {

		public void start(int[] array) {
			ActorRef<Sorter> sorter = Actr.system().actorOf(Sorter::new, "c");
			sorter.<int[]>ask((s, cb) -> s.run(array, cb), this::result);
		}

		public void result(int[] array) {
			Actr.system().shutdown();
			System.err.println(Arrays.toString(array));
		}
	}

	private static class Sorter {

		public void run(int[] array, Consumer<int[]> callback) {
			if (array.length == 1) {
				callback.accept(array);
			} else {
				int[] left  = Arrays.copyOfRange(array, 0, array.length / 2);
				int[] right = Arrays.copyOfRange(array, array.length / 2, array.length);
	
				Actr.system().<Integer, Sorter>forkBuilder()
					.ids(0, 1)
					.constructor(id -> new Sorter())
					.<int[]>ask((id, sorter, cb) -> sorter.run(id == 0 ? left : right, cb), map -> join(map, callback));
			}
		}
		
		private void join(Map<Integer, int[]> map, Consumer<int[]> callback) {
			int[] resultarray = merge(map.get(0), map.get(1));
			callback.accept(resultarray);
		}

		public static int[] merge(int[] a, int[] b) {
			int[] answer = new int[a.length + b.length];
			int i = a.length - 1, j = b.length - 1, k = answer.length;
			while (k > 0)
				answer[--k] = (j < 0 || (i >= 0 && a[i] >= b[j])) ? a[i--] : b[j--];
			return answer;
		}
	
	}

}
