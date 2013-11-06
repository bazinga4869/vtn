/*
 * Copyright (c) 2012-2013 NEC Corporation
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html
 */
#ifndef __VTN_CONF_DATA_ELEMENT_OP_HH__
#define __VTN_CONF_DATA_ELEMENT_OP_HH__

#include<string>
#include "vtn_conf_utility.hh"
#include "confignode.hh"

namespace unc {
namespace vtndrvcache {
template<typename key, typename value, typename op>
class CacheElementUtil: public  ConfigNode {
 private:
  key* key_;
  value* value_;
  op operation;

 public:
  /**
   * @brief  : Constructor to set the key struct, value structure & operation
   */
  CacheElementUtil(key* key_ty, value* value_ty, op opet):
      operation(opet) {
        key_ = new key();
        PFC_ASSERT(key_ != NULL);
        value_ = new value();
        PFC_ASSERT(value_ != NULL);

        memcpy(key_, key_ty, sizeof(key));
        memcpy(value_, value_ty, sizeof(value));
        pfc_log_debug("In constructor %s..", PFC_FUNCNAME);
      }

  /**
   * @brief : Destructor to free the key struct, value structure
   */
  ~CacheElementUtil() {
    pfc_log_debug("In destructor %s..", PFC_FUNCNAME);
    if (key_ != NULL)
      delete key_;

    if (value_ != NULL)
      delete value_;
  }

  /**
   * @brief       : This method returns the Keytype given the key struct
   * @retval      : key_Type
   */
  unc_key_type_t  get_type() {
    pfc_log_debug("In function %s..", PFC_FUNCNAME);
    if (key_ != NULL) {
      return ConfUtil::get_key_type(*key_);
    }
    return  UNC_KT_INVALID;
  }

  /**
   * @brief        : This method returns the search Key given the key struct
   * @retval       : string
   */
  std::string  get_key() {
    pfc_log_debug("In %s function ", PFC_FUNCNAME);
    if (key_ != NULL) {
      return ConfUtil::get_search_key(*key_);
    }
    return "";
  }

  /**
   * @brief       : This method returns the parent Key given the key struct
   * @retval      : string
   */
  std::string  get_parent_key() {
    pfc_log_debug("In %s function ", PFC_FUNCNAME);
    if (key_ != NULL) {
      return ConfUtil::get_parent_key(*key_);
    }
    return "";
  }

  /**
   * @brief        : This method returns the key struct
   * @retval       : key*
   */
  key* getkey() {
    pfc_log_debug("In %s function ", PFC_FUNCNAME);
    return key_;
  }

  /**
   * @brief        : This method returns the value struct
   * @retval       : key*
   */
  value* getval() {
    pfc_log_debug("Entering function %s..", PFC_FUNCNAME);
    return value_;
  }

  /**
   * @brief        : This method returns the operation
   * @retval       : operation
   */
  op get_operation() {
    pfc_log_debug("Entering function %s..", PFC_FUNCNAME);
    return operation;
  }
};
}  // namespace vtndrvcache
}  // namespace unc
#endif