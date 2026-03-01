package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class DishServiceImpl implements DishService {

    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private DishFlavorMapper dishFlavorMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;

   /**
     * 新增菜品，同时插入菜品对应的口味数据
     * @param dishDTO
     */
   @Transactional
    public void saveWithFlavor(DishDTO dishDTO) {

       Dish dish = new Dish();
       BeanUtils.copyProperties(dishDTO,dish);

       // 向菜品表插入一条数据
        dishMapper.insert(dish);

        // 获取insert语句生成的主键值
       Long dishId = dish.getId();

       List<DishFlavor> flavors = dishDTO.getFlavors();
       if (flavors != null && flavors.size() > 0) {
               flavors.forEach(dishFlavor -> {
                       dishFlavor.setDishId(dishId);
               });
               // 向口味表插入n条数据
               dishFlavorMapper.insertBatch(flavors);
       }
   }

   /**
     * 菜品分页查询
     * @param dishPageQueryDTO
     * @return
     */
   public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
       PageHelper.startPage(dishPageQueryDTO.getPage(),dishPageQueryDTO.getPageSize());
       Page<DishVO> page = dishMapper.pageQuery(dishPageQueryDTO);
       return new PageResult(page.getTotal(),page.getResult());
   }

   /**
     * 批量删除菜品
     * @param ids
     */
   public void deleteBatch(List<Long> ids) {
       // 是否起售
       for (Long id : ids) {
           Dish dish = dishMapper.getById(id);
           if (dish.getStatus() == StatusConstant.ENABLE){
               throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
           }
       }

       // 是否关联
       List<Long> setmealIds = setmealDishMapper.getSetmealIdsByDishId(ids);
       if (setmealIds != null && setmealIds.size() > 0) {
           throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
       }

       // 删除菜品
//       for (Long id : ids) {
//           dishMapper.deleteById(id);
//           // 删除口味
//           dishFlavorMapper.deleteByDishId(id);
//       }

       // 根据菜品id集合批量删除菜品数据
       dishMapper.deleteBatch(ids);

       // 根据菜品id集合批量删除菜品口味数据
       dishFlavorMapper.deleteByDishIds(ids);
   }

    /**
     * 根据id查询菜品信息和对应的口味信息
     * @param id
     * @return
     */
   public DishVO getByIdWithFlavor(Long id) {
       // 查询菜品基本信息
       Dish dish = dishMapper.getById(id);
       if (dish == null) {
           return null;
       }

       // 查询菜品对应的口味信息
       List<DishFlavor> flavors = dishFlavorMapper.getByDishId(id);

       // 封装成DishVO对象并返回
       DishVO dishVO = new DishVO();
       BeanUtils.copyProperties(dish,dishVO);
       dishVO.setFlavors(flavors);
       return dishVO;
   }

    /**
     * 根据id更新菜品信息，同时更新对应的口味信息
     * @param dishDTO
     */
    public void updateWithFlavor(DishDTO dishDTO){
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO,dish);

        // 更新菜品基本信息
        dishMapper.update(dish);

        // 删除原有口味数据
        dishFlavorMapper.deleteByDishId(dishDTO.getId());

        // 插入新的口味数据
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors != null && flavors.size() > 0) {
            flavors.forEach(dishFlavor -> {
                dishFlavor.setDishId(dishDTO.getId());
            });
            dishFlavorMapper.insertBatch(flavors);
        }
    }

    /**
     * 修改菜品起售停售
     * @param status
     * @param id
     */
    public void updateStatus(Integer status, Long id) {
        Dish dish = new Dish();
        dish.setId(id);
        dish.setStatus(status);
        dishMapper.update(dish);
    }

    /**
     * 根据分类id查询菜品
     * @param categoryId
     * @return
     */
    public List<Dish> list(Long categoryId) {
        Dish dish = Dish.builder()
                .categoryId(categoryId)
                .status(StatusConstant.ENABLE)
                .build();
        return dishMapper.list(dish);
    }
}
