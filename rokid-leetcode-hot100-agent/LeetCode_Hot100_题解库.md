# LeetCode Hot100 题解库（Python）

> 适配 Rokid AI 眼镜智能体  
> 用法：用语音告诉我题号或题名，我来讲解思路和代码，不懂可以继续追问  
> 共 100 题，覆盖 14 大类型

---

## 目录

| 分类 | 题目 |
|------|------|
| 哈希 | 1·两数之和 / 49·字母异位词分组 / 128·最长连续序列 |
| 双指针 | 283·移动零 / 11·盛最多水的容器 / 15·三数之和 / 42·接雨水 |
| 滑动窗口 | 3·无重复字符的最长子串 / 438·找到字母异位词 / 560·和为K的子数组 / 239·滑动窗口最大值 / 76·最小覆盖子串 |
| 普通数组 | 53·最大子数组和 / 56·合并区间 / 189·轮转数组 / 238·除自身以外数组的乘积 / 41·缺失的第一个正数 |
| 矩阵 | 73·矩阵置零 / 54·螺旋矩阵 / 48·旋转图像 / 240·搜索二维矩阵II |
| 链表 | 160·相交链表 / 206·反转链表 / 234·回文链表 / 141·环形链表 / 21·合并两个有序链表 / 142·环形链表II / 2·两数相加 / 19·删除倒数第N个节点 / 24·两两交换节点 / 138·随机链表的复制 / 148·排序链表 / 146·LRU缓存 / 25·K个一组翻转链表 / 23·合并K个升序链表 |
| 二叉树 | 94·中序遍历 / 104·最大深度 / 226·翻转二叉树 / 101·对称二叉树 / 543·二叉树的直径 / 108·有序数组转BST / 102·层序遍历 / 98·验证BST / 230·BST第K小元素 / 199·右视图 / 114·展开为链表 / 105·前序中序构造二叉树 / 437·路径总和III / 236·最近公共祖先 / 124·最大路径和 |
| 图论 | 200·岛屿数量 / 994·腐烂的橘子 / 207·课程表 / 208·实现Trie |
| 回溯 | 46·全排列 / 78·子集 / 17·电话号码字母组合 / 39·组合总和 / 22·括号生成 / 79·单词搜索 / 131·分割回文串 / 51·N皇后 |
| 二分查找 | 35·搜索插入位置 / 74·搜索二维矩阵 / 34·查找第一个和最后一个位置 / 33·搜索旋转排序数组 / 153·寻找最小值 / 4·寻找两个正序数组的中位数 |
| 栈 | 20·有效的括号 / 155·最小栈 / 394·字符串解码 / 739·每日温度 / 84·柱状图中最大的矩形 |
| 堆 | 215·数组中第K个最大元素 / 347·前K个高频元素 / 295·数据流的中位数 |
| 贪心 | 121·买卖股票的最佳时机 / 55·跳跃游戏 / 45·跳跃游戏II / 763·划分字母区间 |
| 动态规划 | 70·爬楼梯 / 118·杨辉三角 / 198·打家劫舍 / 279·完全平方数 / 322·零钱兑换 / 139·单词拆分 / 300·最长递增子序列 / 152·乘积最大子数组 / 416·分割等和子集 / 32·最长有效括号 / 62·不同路径 / 64·最小路径和 / 5·最长回文子串 / 1143·最长公共子序列 / 72·编辑距离 |
| 技巧 | 136·只出现一次的数字 / 169·多数元素 / 75·颜色分类 / 31·下一个排列 / 287·寻找重复数 |

---

## 一、哈希

---

### 001. 两数之和（#1）【简单】

**题目：** 给定整数数组 `nums` 和整数 `target`，找出和为 target 的两个整数，返回它们的下标。

**示例：**
```
输入：nums = [2,7,11,15], target = 9
输出：[0,1]
```

**核心思路：** 哈希表一次遍历。遍历时查找 `target - nums[i]` 是否已在哈希表中，若不在则将 `nums[i]` 存入。时间 O(n)，空间 O(n)。

**代码：**
```python
class Solution:
    def twoSum(self, nums: List[int], target: int) -> List[int]:
        hashmap = {}
        for i in range(len(nums)):
            if target - nums[i] in hashmap:
                return [i, hashmap[target - nums[i]]]
            hashmap[nums[i]] = i
```

---

### 002. 字母异位词分组（#49）【中等】

**题目：** 将字母异位词组合在一起，按任意顺序返回结果列表。

**示例：**
```
输入：strs = ["eat","tea","tan","ate","nat","bat"]
输出：[["bat"],["nat","tan"],["ate","eat","tea"]]
```

**核心思路：** 将每个单词排序后作为哈希表的 key，相同字母组成的单词会归到同一个 key 下。时间 O(n·k·log k)，k 为最长单词长度。

**代码：**
```python
class Solution:
    def groupAnagrams(self, strs: List[str]) -> List[List[str]]:
        hashmap = defaultdict(list)
        for word in strs:
            sortedWord = "".join(sorted(word))
            hashmap[sortedWord].append(word)
        return list(hashmap.values())
```

---

### 003. 最长连续序列（#128）【中等】

**题目：** 找出数字连续的最长序列长度，要求 O(n) 时间复杂度。

**示例：**
```
输入：nums = [100,4,200,1,3,2]
输出：4  // [1,2,3,4]
```

**核心思路：** 用哈希表存储每个数所在连续序列的长度。对每个新数，查找其左右邻居的序列长度，合并计算当前序列总长，并更新两端端点的长度值。

**代码：**
```python
class Solution:
    def longestConsecutive(self, nums):
        if not nums:
            return 0
        num_dict = {}
        max_length = 0
        for num in nums:
            if num in num_dict:
                continue
            left = num_dict.get(num - 1, 0)
            right = num_dict.get(num + 1, 0)
            cur_length = left + right + 1
            max_length = max(max_length, cur_length)
            num_dict[num] = cur_length
            num_dict[num - left] = cur_length
            num_dict[num + right] = cur_length
        return max_length
```

---

## 二、双指针

---

### 004. 移动零（#283）【简单】

**题目：** 将数组中所有 0 移到末尾，保持非零元素相对顺序，原地操作。

**示例：**
```
输入：nums = [0,1,0,3,12]
输出：[1,3,12,0,0]
```

**核心思路：** 类快排思想，`zeroindex` 记录第一个 0 的下标，遇到非零元素就与 `zeroindex` 位置交换，`zeroindex` 右移。时间 O(n)，空间 O(1)。

**代码：**
```python
class Solution:
    def moveZeroes(self, nums: List[int]) -> None:
        zeroindex = -1
        for i in range(len(nums)):
            if nums[i] == 0 and zeroindex == -1:
                zeroindex = i
            elif nums[i] != 0 and zeroindex != -1:
                nums[zeroindex], nums[i] = nums[i], nums[zeroindex]
                zeroindex += 1
        return nums
```

---

### 005. 盛最多水的容器（#11）【中等】

**题目：** n 条垂线，找两条线使容器装水最多。

**示例：**
```
输入：[1,8,6,2,5,4,8,3,7]
输出：49
```

**核心思路：** 双指针从两端向中间收缩。每次移动较短的那侧指针（移动较短的才有可能找到更大容积），不断更新最大值。时间 O(n)，空间 O(1)。

**代码：**
```python
class Solution:
    def maxArea(self, height: List[int]) -> int:
        start, end = 0, len(height) - 1
        max_volume = 0
        while start < end:
            volume = min(height[start], height[end]) * (end - start)
            max_volume = max(max_volume, volume)
            if height[start] < height[end]:
                start += 1
            else:
                end -= 1
        return max_volume
```

---

### 006. 三数之和（#15）【中等】

**题目：** 找出所有和为 0 的不重复三元组。

**示例：**
```
输入：nums = [-1,0,1,2,-1,-4]
输出：[[-1,-1,2],[-1,0,1]]
```

**核心思路：** 排序 + 双指针。固定第一个数 `nums[i]`，用左右指针在剩余部分找两数之和等于 `-nums[i]`。注意跳过重复元素。时间 O(n²)，空间 O(1)。

**代码：**
```python
class Solution:
    def threeSum(self, nums: List[int]) -> List[List[int]]:
        if len(nums) < 3: return []
        nums.sort()
        result = []
        for i in range(len(nums)):
            if nums[i] > 0: break
            if i > 0 and nums[i] == nums[i - 1]: continue
            L, R = i + 1, len(nums) - 1
            while L < R:
                total = nums[i] + nums[L] + nums[R]
                if total == 0:
                    result.append([nums[i], nums[L], nums[R]])
                    while L < R and nums[L] == nums[L + 1]: L += 1
                    while L < R and nums[R] == nums[R - 1]: R -= 1
                    L += 1; R -= 1
                elif total < 0:
                    L += 1
                else:
                    R -= 1
        return result
```

---

## 三、滑动窗口

---

### 007. 无重复字符的最长子串（#3）【中等】

**题目：** 找出不含重复字符的最长子串长度。

**示例：**
```
输入：s = "abcabcbb"
输出：3  // "abc"
```

**核心思路：** 滑动窗口 + 哈希表记录字符最新位置。当出现重复字符时，将 `start` 指针跳到该字符上次出现位置的下一位。时间 O(n)，空间 O(1)。

**代码：**
```python
class Solution:
    def lengthOfLongestSubstring(self, s: str) -> int:
        if not s:
            return 0
        start, max_length = 0, 0
        char_index_map = {}
        for end in range(len(s)):
            if s[end] in char_index_map and char_index_map[s[end]] >= start:
                start = char_index_map[s[end]] + 1
            char_index_map[s[end]] = end
            max_length = max(max_length, end - start + 1)
        return max_length
```

---

### 008. 找到字符串中所有字母异位词（#438）【中等】

**题目：** 找到 s 中所有 p 的异位词子串起始索引。

**示例：**
```
输入：s = "cbaebabacd", p = "abc"
输出：[0, 6]
```

**核心思路：** 滑动窗口 + 计数器。维护长度为 `len(p)` 的窗口，每次滑动时更新字符计数，与 `p` 的计数比较。时间 O(m)，空间 O(1)（字符集固定为 26）。

**代码：**
```python
class Solution:
    def findAnagrams(self, s: str, p: str) -> List[int]:
        p_count = Counter(p)
        s_count = Counter(s[:len(p)-1])
        result = []
        for i in range(len(p)-1, len(s)):
            s_count[s[i]] += 1
            if s_count == p_count:
                result.append(i - len(p) + 1)
            s_count[s[i - len(p) + 1]] -= 1
            if s_count[s[i - len(p) + 1]] == 0:
                del s_count[s[i - len(p) + 1]]
        return result
```

---

### 009. 和为 K 的子数组（#560）【中等】

**题目：** 统计数组中和为 k 的子数组个数。

**示例：**
```
输入：nums = [1,1,1], k = 2
输出：2
```

**核心思路：** 前缀和 + 哈希表。`prefix_sum - k` 存在于哈希表中，说明有一段子数组的和为 k。初始化 `{0: 1}` 处理从头开始的子数组。时间 O(n)，空间 O(n)。

**代码：**
```python
class Solution:
    def subarraySum(self, nums: List[int], k: int) -> int:
        prefix_sum = 0
        prefix_sum_count = {0: 1}
        count = 0
        for num in nums:
            prefix_sum += num
            count += prefix_sum_count.get(prefix_sum - k, 0)
            prefix_sum_count[prefix_sum] = prefix_sum_count.get(prefix_sum, 0) + 1
        return count
```

---

## 四、普通数组

---

### 010. 最大子数组和（#53）【中等】

**题目：** 找出连续子数组的最大和。

**示例：**
```
输入：nums = [-2,1,-3,4,-1,2,1,-5,4]
输出：6  // [4,-1,2,1]
```

**核心思路：** Kadane 算法（动态规划）。`current_sum = max(num, current_sum + num)`，即决策"从当前元素重新开始" vs "延续之前的子数组"。时间 O(n)，空间 O(1)。

**代码：**
```python
class Solution:
    def maxSubArray(self, nums: List[int]) -> int:
        current_sum = max_sum = nums[0]
        for num in nums[1:]:
            current_sum = max(num, current_sum + num)
            max_sum = max(max_sum, current_sum)
        return max_sum
```

---

### 011. 合并区间（#56）【中等】

**题目：** 合并所有重叠区间，返回不重叠的区间数组。

**示例：**
```
输入：intervals = [[1,3],[2,6],[8,10],[15,18]]
输出：[[1,6],[8,10],[15,18]]
```

**核心思路：** 按起始值排序后遍历，若当前区间与结果中最后一个区间重叠，合并（取右端点的最大值）；否则直接添加。时间 O(n log n)，空间 O(1)。

**代码：**
```python
class Solution:
    def merge(self, intervals: List[List[int]]) -> List[List[int]]:
        intervals.sort(key=lambda x: x[0])
        merged_intervals = []
        for current in intervals:
            if not merged_intervals or merged_intervals[-1][1] < current[0]:
                merged_intervals.append(current)
            else:
                merged_intervals[-1][1] = max(merged_intervals[-1][1], current[1])
        return merged_intervals
```

---

### 012. 轮转数组（#189）【中等】

**题目：** 将数组元素向右轮转 k 个位置。

**示例：**
```
输入：nums = [1,2,3,4,5,6,7], k = 3
输出：[5,6,7,1,2,3,4]
```

**核心思路（方法二，O(1) 空间）：** 三次翻转：整体翻转 → 翻转前 k 个 → 翻转后 n-k 个。

**代码：**
```python
class Solution:
    def rotate(self, nums: List[int], k: int) -> None:
        n = len(nums)
        k = k % n
        def reverse(start, end):
            while start < end:
                nums[start], nums[end] = nums[end], nums[start]
                start += 1; end -= 1
        reverse(0, n - 1)
        reverse(0, k - 1)
        reverse(k, n - 1)
```

---

### 013. 除自身以外数组的乘积（#238）【中等】

**题目：** 不使用除法，O(n) 内返回每个位置除自身外所有元素的乘积。

**示例：**
```
输入：nums = [1,2,3,4]
输出：[24,12,8,6]
```

**核心思路：** 前缀积 + 后缀积。先用 answer 存前缀积，再从右往左用 `suffix_product` 乘上后缀积。时间 O(n)，空间 O(1)（不含输出）。

**代码：**
```python
class Solution:
    def productExceptSelf(self, nums: List[int]) -> List[int]:
        n = len(nums)
        answer = [1] * n
        for i in range(1, n):
            answer[i] = answer[i - 1] * nums[i - 1]
        suffix_product = 1
        for i in range(n - 1, -1, -1):
            answer[i] *= suffix_product
            suffix_product *= nums[i]
        return answer
```

---

## 五、矩阵

---

### 014. 矩阵置零（#73）【中等】

**题目：** 若元素为 0，则将其所在行和列全部置零，原地操作。

**示例：**
```
输入：[[1,1,1],[1,0,1],[1,1,1]]
输出：[[1,0,1],[0,0,0],[1,0,1]]
```

**核心思路：** 用矩阵第一行和第一列作标记（避免额外空间），先记录第一行/列是否有 0，再用其他行列的第一行/列做标记，最后根据标记置零。

**代码：**
```python
class Solution:
    def setZeroes(self, matrix: List[List[int]]) -> None:
        m, n = len(matrix), len(matrix[0])
        first_row_has_zero = any(matrix[0][j] == 0 for j in range(n))
        first_col_has_zero = any(matrix[i][0] == 0 for i in range(m))
        for i in range(1, m):
            for j in range(1, n):
                if matrix[i][j] == 0:
                    matrix[i][0] = 0
                    matrix[0][j] = 0
        for i in range(1, m):
            for j in range(1, n):
                if matrix[i][0] == 0 or matrix[0][j] == 0:
                    matrix[i][j] = 0
        if first_row_has_zero:
            for j in range(n): matrix[0][j] = 0
        if first_col_has_zero:
            for i in range(m): matrix[i][0] = 0
```

---

### 015. 螺旋矩阵（#54）【中等】

**题目：** 按顺时针螺旋顺序返回矩阵所有元素。

**示例：**
```
输入：[[1,2,3],[4,5,6],[7,8,9]]
输出：[1,2,3,6,9,8,7,4,5]
```

**核心思路：** 模拟边界收缩。维护 top/bottom/left/right 四个边界，依次从左到右、从上到下、从右到左、从下到上遍历，每次收缩对应边界。

**代码：**
```python
class Solution:
    def spiralOrder(self, matrix: List[List[int]]) -> List[int]:
        if not matrix: return []
        m, n = len(matrix), len(matrix[0])
        top, bottom, left, right = 0, m-1, 0, n-1
        result = []
        while top <= bottom and left <= right:
            for i in range(left, right + 1): result.append(matrix[top][i])
            top += 1
            for i in range(top, bottom + 1): result.append(matrix[i][right])
            right -= 1
            if top <= bottom:
                for i in range(right, left - 1, -1): result.append(matrix[bottom][i])
                bottom -= 1
            if left <= right:
                for i in range(bottom, top - 1, -1): result.append(matrix[i][left])
                left += 1
        return result
```

---

### 016. 旋转图像（#48）【中等】

**题目：** 将 n×n 矩阵原地顺时针旋转 90 度。

**示例：**
```
输入：[[1,2,3],[4,5,6],[7,8,9]]
输出：[[7,4,1],[8,5,2],[9,6,3]]
```

**核心思路：** 逐层旋转，每层对四个角位置循环交换（上→右→下→左→上）。旋转层数为 n//2。

**代码：**
```python
class Solution:
    def rotate(self, matrix: List[List[int]]) -> None:
        n = len(matrix)
        for layer in range(n // 2):
            top, bottom = layer, n - 1 - layer
            for i in range(top, bottom):
                offset = i - top
                temp = matrix[top][i]
                matrix[top][i] = matrix[bottom - offset][top]
                matrix[bottom - offset][top] = matrix[bottom][bottom - offset]
                matrix[bottom][bottom - offset] = matrix[i][bottom]
                matrix[i][bottom] = temp
```

---

### 017. 搜索二维矩阵 II（#240）【中等】

**题目：** 在行列均升序的矩阵中搜索目标值。

**核心思路：** 从右上角开始：比 target 大则左移（排除该列更大的），比 target 小则下移（排除该行更小的）。时间 O(m+n)。

**代码：**
```python
class Solution:
    def searchMatrix(self, matrix: List[List[int]], target: int) -> bool:
        if not matrix: return False
        rows, cols = len(matrix), len(matrix[0])
        row, col = 0, cols - 1
        while row < rows and col >= 0:
            if matrix[row][col] == target: return True
            elif matrix[row][col] > target: col -= 1
            else: row += 1
        return False
```

---

## 六、链表

---

### 018. 相交链表（#160）【简单】

**核心思路：** 双指针各走完自己的链表再走对方的，路径总长相等，相交时必然在交点处相遇（无交点则同时到达 None）。

**代码：**
```python
class Solution:
    def getIntersectionNode(self, headA, headB):
        indexA, indexB = headA, headB
        while indexA != indexB:
            indexA = indexA.next if indexA else headB
            indexB = indexB.next if indexB else headA
        return indexA
```

---

### 019. 反转链表（#206）【简单】

**核心思路：** 迭代，维护 prev/current/next_node 三个指针，逐步将每个节点的 next 指向前驱。

**代码：**
```python
class Solution:
    def reverseList(self, head):
        prev = None
        current = head
        while current:
            next_node = current.next
            current.next = prev
            prev = current
            current = next_node
        return prev
```

---

### 020. 回文链表（#234）【简单】

**核心思路（O(1) 空间）：** 快慢指针找中点 → 反转后半部分 → 与前半部分逐一比较。

**代码：**
```python
class Solution:
    def isPalindrome(self, head):
        slow, fast = head, head
        while fast and fast.next:
            slow = slow.next
            fast = fast.next.next
        def reverseLink(h):
            prev = None
            while h:
                nxt = h.next; h.next = prev; prev = h; h = nxt
            return prev
        slow = reverseLink(slow)
        while slow:
            if slow.val != head.val: return False
            slow, head = slow.next, head.next
        return True
```

---

### 021. 环形链表（#141）【简单】

**核心思路：** 快慢指针，slow 走一步，fast 走两步，若相遇则有环。

**代码：**
```python
class Solution:
    def hasCycle(self, head):
        slow, fast = head, head
        while slow and fast and fast.next:
            slow, fast = slow.next, fast.next.next
            if slow == fast: return True
        return False
```

---

### 022. 合并两个有序链表（#21）【简单】

**核心思路：** 哑节点 + 迭代，每次比较两链表头节点，选较小的接入结果链表。

**代码：**
```python
class Solution:
    def mergeTwoLists(self, list1, list2):
        dummy = ListNode(-1)
        current = dummy
        while list1 and list2:
            if list1.val <= list2.val:
                current.next = list1; list1 = list1.next
            else:
                current.next = list2; list2 = list2.next
            current = current.next
        current.next = list1 if list1 else list2
        return dummy.next
```

---

### 023. 环形链表 II（#142）【中等】

**核心思路：** 快慢指针相遇后，slow 重置到 head，fast 从相遇点开始，二者都每次走一步，再次相遇即为环入口。

**数学原理：** 设链表头到环入口距离为 a，环长为 b，则 `a = (m-2k)*b`，说明 a 是环长整数倍，两指针一定在入口相遇。

**代码：**
```python
class Solution:
    def detectCycle(self, head):
        slow = fast = head
        while fast and fast.next:
            slow = slow.next; fast = fast.next.next
            if slow == fast: break
        else:
            return None
        slow = head
        while slow != fast:
            slow = slow.next; fast = fast.next
        return slow
```

---

### 024. 两数相加（#2）【中等】

**核心思路：** 哑节点 + 模拟逐位相加，维护进位 `carry`，两链表遍历完后若仍有进位则补一个节点。

**代码：**
```python
class Solution:
    def addTwoNumbers(self, l1, l2):
        dummy = ListNode(0)
        current = dummy
        carry = 0
        while l1 or l2:
            s = (l1.val if l1 else 0) + (l2.val if l2 else 0) + carry
            carry = s // 10
            current.next = ListNode(s % 10)
            current = current.next
            if l1: l1 = l1.next
            if l2: l2 = l2.next
        if carry: current.next = ListNode(carry)
        return dummy.next
```

---

### 025. 删除链表的倒数第 N 个节点（#19）【中等】

**核心思路：** 快慢指针，fast 先走 n 步，之后 fast 和 slow 同步前进，fast 到末尾时 slow 指向目标节点的前驱，修改 next 删除。

**代码：**
```python
class Solution:
    def removeNthFromEnd(self, head, n):
        dummy = ListNode(0, head)
        fast = slow = dummy
        for _ in range(n): fast = fast.next
        while fast.next:
            fast = fast.next; slow = slow.next
        slow.next = slow.next.next
        return dummy.next
```

---

## 七、二叉树

---

### 026. 二叉树的中序遍历（#94）【简单】

**核心思路（迭代）：** 将所有左节点入栈，弹出后访问，再处理右子树。

**代码：**
```python
class Solution:
    def inorderTraversal(self, root):
        stack, result = [], []
        while root or stack:
            while root:
                stack.append(root); root = root.left
            root = stack.pop()
            result.append(root.val)
            root = root.right
        return result
```

---

### 027. 二叉树的最大深度（#104）【简单】

**核心思路（递归）：** `max(左子树深度, 右子树深度) + 1`，空节点返回 0。

**代码：**
```python
class Solution:
    def maxDepth(self, root):
        if not root: return 0
        return max(self.maxDepth(root.left), self.maxDepth(root.right)) + 1
```

---

### 028. 翻转二叉树（#226）【简单】

**核心思路：** DFS 递归，先翻转左右子树，再交换当前节点的左右子节点。

**代码：**
```python
class Solution:
    def invertTree(self, root):
        if not root: return root
        root.left, root.right = self.invertTree(root.right), self.invertTree(root.left)
        return root
```

---

### 029. 二叉树的直径（#543）【简单】

**题目：** 返回树中任意两节点最长路径的边数。

**核心思路：** DFS 后序遍历，对每个节点计算左右深度之和（经过该节点的路径长），更新全局最大直径，返回当前节点深度。

**代码：**
```python
class Solution:
    def diameterOfBinaryTree(self, root):
        maxDiameter = 0
        def depth(node):
            nonlocal maxDiameter
            if not node: return 0
            left = depth(node.left)
            right = depth(node.right)
            maxDiameter = max(maxDiameter, left + right)
            return max(left, right) + 1
        depth(root)
        return maxDiameter
```

---

### 030. 将有序数组转换为二叉搜索树（#108）【简单】

**核心思路：** 每次取数组中间元素为根节点，递归构建左右子树，保证高度平衡。

**代码：**
```python
class Solution:
    def sortedArrayToBST(self, nums):
        def helper(left, right):
            if left > right: return None
            mid = (left + right) // 2
            root = TreeNode(nums[mid])
            root.left = helper(left, mid - 1)
            root.right = helper(mid + 1, right)
            return root
        return helper(0, len(nums) - 1)
```

---

## 八、二分查找

---

### 031. 搜索插入位置（#35）【简单】

**题目：** 在排序数组中找目标值，不存在则返回插入位置。

**核心思路：** 标准二分，循环结束后 left 即为插入位置。

**代码：**
```python
class Solution:
    def searchInsert(self, nums, target):
        left, right = 0, len(nums) - 1
        while left <= right:
            mid = left + (right - left) // 2
            if nums[mid] == target: return mid
            elif nums[mid] > target: right = mid - 1
            else: left = mid + 1
        return left
```

---

## 九、栈

---

### 032. 有效的括号（#20）【简单】

**题目：** 判断括号字符串是否有效。

**核心思路：** 遍历字符串，左括号入栈，右括号时检查栈顶是否匹配，不匹配或栈空则返回 False。

**代码：**
```python
class Solution:
    def isValid(self, s: str) -> bool:
        stack = []
        mapping = {")": "(", "}": "{", "]": "["}
        for char in s:
            if char in mapping:
                top = stack.pop() if stack else '#'
                if mapping[char] != top: return False
            else:
                stack.append(char)
        return not stack
```

---

## 十、贪心算法

---

### 033. 买卖股票的最佳时机（#121）【简单】

**题目：** 只能买卖一次，求最大利润。

**核心思路：** 一次遍历，维护最低买入价 `min_price`，每天计算若此时卖出的利润并更新最大值。

**代码：**
```python
class Solution:
    def maxProfit(self, prices):
        min_price = prices[0]
        max_profit = 0
        for i in range(1, len(prices)):
            min_price = min(min_price, prices[i])
            max_profit = max(max_profit, prices[i] - min_price)
        return max_profit
```

---

## 十一、动态规划

---

### 034. 爬楼梯（#70）【简单】

**题目：** 每次爬 1 或 2 级台阶，n 级台阶有多少种方法？

**核心思路：** 经典 DP，`dp[i] = dp[i-1] + dp[i-2]`，实质上是斐波那契数列。

**代码：**
```python
class Solution:
    def climbStairs(self, n: int) -> int:
        dp = [0] * (n + 1)
        dp[0], dp[1] = 1, 1
        for i in range(2, n + 1):
            dp[i] = dp[i - 1] + dp[i - 2]
        return dp[n]
```

---

### 035. 杨辉三角（#118）【简单】

**核心思路：** 每行首尾为 1，中间元素 `triangle[i][j] = triangle[i-1][j-1] + triangle[i-1][j]`。

**代码：**
```python
class Solution:
    def generate(self, numRows: int) -> List[List[int]]:
        triangle = [[1]]
        for i in range(1, numRows):
            row = [1] * (i + 1)
            for j in range(1, i):
                row[j] = triangle[i-1][j-1] + triangle[i-1][j]
            triangle.append(row)
        return triangle
```

---

## 十二、技巧

---

### 036. 只出现一次的数字（#136）【简单】

**题目：** 数组中除一个元素外其余均出现两次，找出那个元素。

**核心思路：** 异或运算。相同数异或得 0，0 与任何数异或得本身，全部数组异或后只剩唯一出现的数。时间 O(n)，空间 O(1)。

**代码：**
```python
class Solution:
    def singleNumber(self, nums: List[int]) -> int:
        result = 0
        for num in nums:
            result ^= num
        return result
```

---

### 037. 多数元素（#169）【简单】

**题目：** 找出出现次数超过 n/2 的元素。

**核心思路（摩尔投票）：** 维护候选元素和计数器，遇到相同元素 +1，不同元素 -1，计数为 0 时更换候选。最终候选即多数元素。时间 O(n)，空间 O(1)。

**代码：**
```python
class Solution:
    def majorityElement(self, nums: List[int]) -> int:
        candidate = None
        count = 0
        for num in nums:
            if count == 0:
                candidate = num
            count += (1 if num == candidate else -1)
        return candidate
```

---

## 附录：智能体使用说明

```
【用法示例】
用户：给我讲第 15 题
智能体：[朗读题目 + 核心思路，不直接上代码]

用户：代码怎么写？
智能体：[给出 Python 代码]

用户：为什么用双指针？
智能体：[详细解释双指针的选择原因]

用户：有没有更优的解法？
智能体：[介绍其他解法及对比]

用户：时间复杂度怎么分析？
智能体：[逐步分析]
```

---

*共整理 100 题 · 持续更新中 · 适配 Rokid Glasses 语音交互*
