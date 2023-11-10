package mt.spring.mos.base.algorithm.weight;

import mt.spring.mos.base.utils.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @Author Martin
 * @Date 2020/11/29
 */
public class WeightAlgorithm<T extends WeightAble> {
	private final List<WeightWrapper> weightWrappers;
	private final int max;
	
	public WeightAlgorithm(List<? extends WeightAble> targets) {
		Assert.notEmpty(targets, "targets can not be empty");
		boolean hasGtZero = targets.stream().anyMatch(weightAble -> weightAble.getWeight() > 0);
		int index = 0;
		weightWrappers = new ArrayList<>(targets.size());
		for (WeightAble target : targets) {
			int weight = hasGtZero ? target.getWeight() : 10;
			WeightWrapper weightWrapper = new WeightWrapper(index, index + weight, target);
			weightWrappers.add(weightWrapper);
			index += weight;
		}
		max = index;
	}
	
	@SuppressWarnings("unchecked")
	public T weightRandom() {
		if (weightWrappers.size() == 1) {
			return (T) weightWrappers.get(0).getTarget();
		}
		Random random = new Random();
		int i = random.nextInt(max);
		return (T) weightWrappers.stream().filter(weightWrapper -> weightWrapper.isHit(i)).findFirst().orElseThrow(RuntimeException::new).getTarget();
	}
	
}
